package lorry.dossiertau.ui.bottomSheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import lorry.dossiertau.MainActivity
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.support.BottomSheetType
import lorry.dossiertau.ui.bottomSheet.support.IBrowser
import org.koin.android.ext.android.inject

@Composable
fun MainActivity.BottomSheetContent(
    type: BottomSheetType,
    item: TauItem?,
    sheetText: MutableState<String>,
    isSheetVisible: MutableState<Boolean>
) {
    val browser: IBrowser by inject()

    when (type) {
        BottomSheetType.APPLICATION -> {
            ApplicationContent(
                viewModel = viewModel,
                sheetText = sheetText,
                isSheetVisible = isSheetVisible
            )
        }


        BottomSheetType.ITEM -> {

            ItemContent(
                item = item,
                browser = browser
            )
        }
    }
}