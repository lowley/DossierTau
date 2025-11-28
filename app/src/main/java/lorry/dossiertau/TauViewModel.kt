package lorry.dossiertau

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import org.koin.core.KoinApplication

class TauViewModel(
    val folderCompo: IFolderCompo
): ViewModel() {

    //////////////////////////
    // currentFoldePathText //
    //////////////////////////
    private val _currentFoldePath = MutableStateFlow<TauPath>(TauPath.of("/storage/emulated/0/Download"))
    val currentFoldePath = _currentFoldePath.asStateFlow()

    fun setCurrentFoldePath(path: TauPath){
        _currentFoldePath.value = path
    }

    fun setTauFolder(folderPath: TauPath){
        folderCompo.setFolderFlow(folderPath)
    }


}