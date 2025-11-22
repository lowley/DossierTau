package lorry.dossiertau

import androidx.lifecycle.ViewModel
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.usecases.folderContent.IFolderCompo

class TauViewModel(
    val folderCompo: IFolderCompo
): ViewModel() {

    fun setTauFolder(folderPath: TauPath){
        folderCompo.setFolderFlow(folderPath)
    }


}