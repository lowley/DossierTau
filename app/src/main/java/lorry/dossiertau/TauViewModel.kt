package lorry.dossiertau

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.generateHTMLs.Links

open class TauViewModel(
    val folderCompo: IFolderCompo,
    val spy: ISpy,
    val links: Links
): ViewModel() {

    fun setTauFolder(folderPath: TauPath){
        folderCompo.setFolderFlow(folderPath)
        spy.setObservedFolder(folderPath)
        if (!spy.enabledFlow.value)
            spy.startSurveillance()
    }

    fun onMakeHTML() {
        viewModelScope.launch{
            links.generateLinks()
        }
    }

    //#[[tauViewModelInit]]
    init{
        val pathInit = "/storage/emulated/0/Movies/sexe/filles".toTauPath()
        println("TauViewModel: init{} appelle setTauFolder")
        setTauFolder(pathInit)

    }

}