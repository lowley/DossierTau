package lorry.dossiertau.ui.bottomSheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.skydoves.flexible.core.FlexibleSheetState
import lorry.dossiertau.MainActivity
import lorry.dossiertau.ShortcutMakingEndMessage
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.support.BottomSheetType
import lorry.dossiertau.ui.bottomSheet.support.GestureOwner
import lorry.dossiertau.ui.bottomSheet.support.IBrowser

@Composable
fun MainActivity.BottomSheetContent(
    type: BottomSheetType?,
    item: TauItem?,
    sheetText: MutableState<String>,
    sheetState: FlexibleSheetState,
    browser: IBrowser,
    gestureOwner: MutableState<GestureOwner>
    ) {

    when (type) {
        BottomSheetType.APPLICATION -> {
            ApplicationContent(
                viewModel = viewModel,
                sheetText = sheetText,
                sheetState = sheetState,
                shortcutMakingEndMessage = ShortcutMakingEndMessage
            )
        }

        BottomSheetType.ITEM -> {
            ItemContent(
                item = item,
                browser = browser,
                sheetState = sheetState,
                tauvm = viewModel,
                gestureOwner = gestureOwner
            )
        }

        else -> {}
    }
}