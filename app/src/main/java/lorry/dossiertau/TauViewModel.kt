package lorry.dossiertau

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.generateHTMLs.Links
import lorry.dossiertau.usecases.generateHTMLs.logSummary
import lorry.folder.items.dossiersigma.external.playing.IPlayingDataSource

open class TauViewModel(
    val folderCompo: IFolderCompo,
    val spy: ISpy,
    val links: Links,
    val playingFile: IPlayingDataSource
): ViewModel() {

    var translator: Translator? = null

    fun setTauFolder(folderPath: TauPath){
        folderCompo.setFolderFlow(folderPath)
        spy.setObservedFolder(folderPath)
        if (!spy.enabledFlow.value)
            spy.startSurveillance()
    }

    fun onMakeHTML(
        displayBottomSheet: (text: String) -> Unit = {},
    ) {
        viewModelScope.launch{
            links.generateLinks()
        }

//        translationExample(
//            displayBottomSheet = displayBottomSheet
//        )
    }

    private fun translationExample(displayBottomSheet: (String) -> Unit) {
        val result = translator?.translate("<p>Come and have your way with Jamie LaMore, London Keys, Sadie West, Taylor Tilden, and Kiara Dinae. These five girls are tied up and helpless. They're just waiting to be teased and fucked beyond comprehension. Show them total domination, and they will return the favor with pure satisfaction. Their lustful natures are beyond measure, for these girls are bound for your pleasure.</p> <p><strong>Bonus Footage Included</strong></p>")
            ?.addOnSuccessListener { result ->
                displayBottomSheet(result ?: "erreur de traduction")
            }
            ?.addOnFailureListener { exception ->
                displayBottomSheet(exception.toString())
            }
    }

    //#[[tauViewModelInit]]
    init{
        val pathInit = "/storage/emulated/0/Movies/sexe".toTauPath()
        println("TauViewModel: init{} appelle setTauFolder")
        setTauFolder(pathInit)

        configureTranslation()
    }

    private fun configureTranslation() {
        // Créer le traducteur
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.FRENCH)
            .build()
        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener { logSummary("prêt à traduire") }
            ?.addOnFailureListener { logSummary("erreur de téléchargement") }
    }
}