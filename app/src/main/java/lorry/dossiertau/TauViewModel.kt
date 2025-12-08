package lorry.dossiertau

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.folderContent.IFolderCompo

class TauViewModel(
    val folderCompo: IFolderCompo,
    val spy: ISpy
): ViewModel() {

    fun setTauFolder(folderPath: TauPath){
        folderCompo.setFolderFlow(folderPath)
        spy.setObservedFolder(folderPath)
        if (!spy.enabledFlow.value)
            spy.startSurveillance()
    }

    //#[[tauViewModelInit]]
    init{
        val pathInit = "/storage/emulated/0/Download".toTauPath()
        setTauFolder(pathInit)

    }

}