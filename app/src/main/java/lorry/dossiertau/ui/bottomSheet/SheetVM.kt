package lorry.dossiertau.ui.bottomSheet

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lorry.dossiertau.ui.bottomSheet.support.SheetType

class SheetVM: ViewModel() {

    private val _type: MutableStateFlow<SheetType?> = MutableStateFlow(SheetType.NONE)
    val type = _type.asStateFlow()

    fun changeType(newType: SheetType) {
        _type.update { newType }
    }
}