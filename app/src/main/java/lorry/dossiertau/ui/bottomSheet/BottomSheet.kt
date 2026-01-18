package lorry.dossiertau.ui.bottomSheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import lorry.dossiertau.MainActivity
import lorry.dossiertau.ShortcutMakingEndMessage
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.support.BottomSheetType
import lorry.dossiertau.ui.bottomSheet.support.GestureOwner
import lorry.dossiertau.ui.bottomSheet.support.IBrowser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.BottomSheetContent(
    modifier: Modifier = Modifier,
    type: BottomSheetType?,
    item: TauItem?,
    sheetText: MutableState<String>,
    sheetState: SheetState,
    browser: IBrowser,
    gestureOwner: MutableState<GestureOwner>,
    changeSheetType: (BottomSheetType) -> Unit = {}
    ) {

    when (type) {
        BottomSheetType.APPLICATION -> {
            ApplicationContent(
                modifier = modifier,
                viewModel = viewModel,
                sheetText = sheetText,
                sheetState = sheetState,
                shortcutMakingEndMessage = ShortcutMakingEndMessage
            )
        }

        BottomSheetType.ITEM -> {
            ItemContent(
                modifier = modifier,
                item = item,
                browser = browser,
                sheetState = sheetState,
                tauvm = viewModel,
                gestureOwner = gestureOwner,
                changeSheetType = changeSheetType
            )
        }

        else -> {}
    }
}