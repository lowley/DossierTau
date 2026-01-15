package lorry.dossiertau.ui.bottomSheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import lorry.dossiertau.MainActivity
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.support.BottomSheetType

@Composable
fun MainActivity.BottomSheetContent(
    type: BottomSheetType,
    item: TauItem?,
    sheetText: MutableState<String>,
    isSheetVisible: MutableState<Boolean>
) {
    when (type) {
        BottomSheetType.APPLICATION -> {
            ApplicationContent(
                viewModel = viewModel,
                sheetText = sheetText,
                isSheetVisible = isSheetVisible
            )
        }


        BottomSheetType.ITEM -> {
            ItemContent(item)
        }
    }
}