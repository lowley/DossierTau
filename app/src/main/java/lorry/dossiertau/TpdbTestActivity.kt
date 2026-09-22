package lorry.dossiertau

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.text.Normalizer
import kotlin.math.max

class TpdbTestActivity : ComponentActivity() {
    private lateinit var tokenField: EditText
    private lateinit var status: TextView
    private lateinit var image: ImageView
    private val filename by lazy { intent.getStringExtra(EXTRA_FILENAME).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("tpdb", MODE_PRIVATE)
        val parsed = parseFilename(filename)
        tokenField = EditText(this).apply { hint="Token TPDB"; setText(prefs.getString("api_token", "")); inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        status=TextView(this); image=ImageView(this).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER}
        val search=Button(this).apply{text="Rechercher l'image dans TPDB";setOnClickListener{val token=tokenField.text.toString().trim();if(token.isBlank()){status.text="Entre d'abord le token TPDB.";return@setOnClickListener};prefs.edit().putString("api_token",token).apply();runSearch(token)}}
        val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;val p=(16*resources.displayMetrics.density).toInt();setPadding(p,p,p,p);addView(TextView(context).apply{text="Test TPDB\n\nFichier : $filename\nTitre recherché : ${parsed.title}${parsed.year?.let { "\nAnnée du fichier (indice seulement) : $it" } ?: ""}";textSize=18f});addView(tokenField);addView(search);addView(status);addView(image,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))}
        setContentView(ScrollView(this).apply{addView(content)});if(tokenField.text.isNotBlank()&&filename.isNotBlank())runSearch(tokenField.text.toString().trim())
    }

    private fun runSearch(token:String){val parsed=parseFilename(filename);status.text="Recherche…\nTitre envoyé à TPDB : ${parsed.title}\nAnnée non envoyée";image.setImageDrawable(null);lifecycleScope.launch{val result=runCatching{withContext(Dispatchers.IO){searchTpdb(filename,token)}};result.fold(onSuccess={r->if(r==null)status.text="Aucune correspondance suffisamment proche.\nTitre envoyé à TPDB : ${parsed.title}\nAnnée non envoyée" else{status.text="Trouvé : ${r.title}\nTitre envoyé à TPDB : ${r.query}\nScore : ${r.score}\nAnnée TPDB : ${r.tpdbYear ?: "inconnue"}\nAnnée fichier : ${parsed.year ?: "inconnue"}\nSite/studio : ${r.site ?: "inconnu"}\nImage : ${r.imageSource}\nType : film";lifecycleScope.launch{val bitmap=withContext(Dispatchers.IO){URL(r.imageUrl).openStream().use(BitmapFactory::decodeStream)};image.setImageBitmap(bitmap)}}},onFailure={status.text="Erreur TPDB : ${it.message?:it.javaClass.simpleName}\nTitre envoyé à TPDB : ${parsed.title}"})}}

    private fun searchTpdb(filename:String,token:String):Result?{
        val parsed=parseFilename(filename)
        val params="parse=${URLEncoder.encode(parsed.title,"UTF-8")}&per_page=10"
        val connection=URL("https://api.theporndb.net/movies?$params").openConnection() as HttpURLConnection
        connection.connectTimeout=4000;connection.readTimeout=8000;connection.setRequestProperty("Authorization","Bearer $token");connection.setRequestProperty("Accept","application/json")
        val code=connection.responseCode;if(code==401||code==403)error("Token refusé (HTTP $code)");if(code !in 200..299)return null
        val json=connection.inputStream.bufferedReader().use{JSONObject(it.readText())};val data=json.optJSONArray("data")?:return null
        val candidates=mutableListOf<Result>()
        for(i in 0 until data.length()){
            val obj=data.getJSONObject(i)
            if(!obj.optString("type").equals("Movie",ignoreCase=true)) continue
            val title=obj.optString("title").trim();if(title.isBlank())continue
            val image=chooseImage(obj)?:continue
            val tpdbYear=obj.optString("date").take(4).toIntOrNull()
            val site=obj.optJSONObject("site")?.optString("name")?.takeIf{it.isNotBlank()}
            val score=matchScore(parsed.title,title,parsed.year,tpdbYear)
            candidates += Result(title,image.first,parsed.title,score,tpdbYear,site,image.second)
        }
        val best=candidates.maxByOrNull{it.score}?:return null
        return best.takeIf{it.score>=60}
    }

    private fun chooseImage(obj:JSONObject):Pair<String,String>?{
        val posters=obj.optJSONObject("posters")
        val choices=listOf(
            posters?.optString("large") to "posters.large",
            posters?.optString("medium") to "posters.medium",
            posters?.optString("full") to "posters.full",
            obj.optString("poster") to "poster",
            obj.optString("poster_image") to "poster_image",
            obj.optString("image") to "image"
        )
        return choices.firstOrNull{!it.first.isNullOrBlank()}?.let{it.first!! to it.second}
    }

    private fun matchScore(query:String,candidate:String,fileYear:Int?,tpdbYear:Int?):Int{
        val a=normalizeTitle(query);val b=normalizeTitle(candidate)
        if(a.isBlank()||b.isBlank())return 0
        var score=when{
            a==b->100
            a.replace(" ","")==b.replace(" ","")->98
            a.contains(b)||b.contains(a)->85
            else->(tokenSimilarity(a,b)*80).toInt()
        }
        if(fileYear!=null&&tpdbYear!=null){val d=kotlin.math.abs(fileYear-tpdbYear);score+=when(d){0->8;1->4;else->0}}
        return score.coerceAtMost(108)
    }

    private fun tokenSimilarity(a:String,b:String):Double{
        val aa=a.split(' ').filter{it.isNotBlank()}.toSet();val bb=b.split(' ').filter{it.isNotBlank()}.toSet();if(aa.isEmpty()||bb.isEmpty())return 0.0
        val intersection=aa.intersect(bb).size.toDouble();return 2.0*intersection/(aa.size+bb.size)
    }

    private fun normalizeTitle(value:String):String{
        val ascii=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
        return ascii.lowercase().replace("&"," and ").replace(Regex("[^a-z0-9]+")," ").trim().replace(Regex("\\s+")," ")
    }

    private fun parseFilename(filename:String):Parsed{var s=filename.substringAfterLast('/').trim();s=s.replace(Regex("(?i)\\.(mp4|mkv|avi|mov|wmv|m4v|webm|ts|m3u8)$"),"");s=s.replace(Regex("\\s*\\(\\d+\\)\\s*$"),"");s=s.replace(Regex("(?i)\\bVol\\.\\s*(\\d+)"),"Vol $1");val yearMatch=Regex("\\((19|20)\\d{2}\\)").find(s);val year=yearMatch?.value?.filter(Char::isDigit)?.toIntOrNull();s=s.replace(Regex("\\s*\\((19|20)\\d{2}\\)\\s*")," ");s=s.replace(Regex("(?i)\\s+by\\s+.*$"),"");s=s.substringBefore('.');val the=Regex("(?i)^(.+),\\s*The$").matchEntire(s.trim());if(the!=null)s="The ${the.groupValues[1]}";s=s.replace('_',' ').replace(Regex("\\s+")," ").trim();return Parsed(s,year)}
    private data class Parsed(val title:String,val year:Int?)
    private data class Result(val title:String,val imageUrl:String,val query:String,val score:Int,val tpdbYear:Int?,val site:String?,val imageSource:String)
    companion object{const val EXTRA_FILENAME="filename"}
}
