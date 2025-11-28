package lorry.dossiertau

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.folderContent.IFolderCompo

class TauViewModel(
    val folderCompo: IFolderCompo
): ViewModel() {

    fun setTauFolder(folderPath: TauPath){
        folderCompo.setFolderFlow(folderPath)
    }

    init{
        setTauFolder("/storage/emulated/0/Download".toTauPath())
    }

}