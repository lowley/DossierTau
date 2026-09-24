package lorry.dossiertau

import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.data.model.TauItem
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
    val _selectedItem = MutableStateFlow<TauItem?>(null)
    val selectedItem: StateFlow<TauItem?> = _selectedItem.asStateFlow()

    fun setSelectedItem(item: TauItem) {
        _selectedItem.update { item }
    }

    private val navigationPreferences by lazy {
        TauApp.instance.getSharedPreferences(NAVIGATION_PREFS, Context.MODE_PRIVATE)
    }

    fun setTauFolder(folderPath: TauPath){
        navigationPreferences.edit()
            .putString(LAST_FOLDER_KEY, folderPath.path)
            .apply()

        folderCompo.setFolderFlow(folderPath)
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
        val defaultPath = "/storage/emulated/0/Movies/sexe"
        val restoredPath = navigationPreferences
            .getString(LAST_FOLDER_KEY, null)
            ?.takeIf { it.isNotBlank() }

        val pathInit = (restoredPath ?: defaultPath).toTauPath()
        println("TauViewModel: init{} restaure le dossier: ${pathInit.path}")
        setTauFolder(pathInit)

        configureTranslation()
    }

    companion object {
        private const val NAVIGATION_PREFS = "tau_navigation"
        private const val LAST_FOLDER_KEY = "last_folder_path"
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