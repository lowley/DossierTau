package lorry.dossiertau.ui.bottomSheet

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lorry.dossiertau.ui.bottomSheet.support.BottomSheetType

class SheetVM: ViewModel() {

    private val _type: MutableStateFlow<BottomSheetType?> = MutableStateFlow(BottomSheetType.NONE)
    val type = _type.asStateFlow()

    fun changeType(newType: BottomSheetType) {
        _type.update { newType }
    }
}