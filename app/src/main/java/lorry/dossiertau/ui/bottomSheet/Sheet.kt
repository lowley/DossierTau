package lorry.dossiertau.ui.bottomSheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import lorry.dossiertau.MainActivity
import lorry.dossiertau.ShortcutMakingEndMessage
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.contents.ContentApplication
import lorry.dossiertau.ui.bottomSheet.contents.ContentItem
import lorry.dossiertau.ui.bottomSheet.support.SheetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.Sheet(
    modifier: Modifier = Modifier,
    type: SheetType?,
    item: TauItem?,
    sheetText: MutableState<String>,
    sheetState: SheetState,
    ) {

    when (type) {
        SheetType.APPLICATION -> {
            ContentApplication(
                modifier = modifier,
                viewModel = viewModel,
                sheetText = sheetText,
                sheetState = sheetState,
                shortcutMakingEndMessage = ShortcutMakingEndMessage
            )
        }

        SheetType.ITEM -> {
            ContentItem(
                modifier = modifier,
                item = item,
                sheetState = sheetState,
                tauvm = viewModel,
            )
        }

        else -> {}
    }
}