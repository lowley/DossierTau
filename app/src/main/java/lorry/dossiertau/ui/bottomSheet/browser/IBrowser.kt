package lorry.dossiertau.ui.bottomSheet.browser

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.browser.support.BrowserState
import lorry.dossiertau.ui.bottomSheet.browser.support.BrowserTarget
import lorry.dossiertau.ui.bottomSheet.support.SheetType

interface IBrowser {

    val vm: BrowserVM

    @Composable
    fun rememberBrowserState(): BrowserState

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Render(
        modifier: Modifier,
        exitImageSelection: () -> Unit
    )

    @OptIn(ExperimentalMaterial3Api::class)
    fun manageImageClick(
        viewModel: TauViewModel,
        imageUrl: String,
        sheetState: SheetState,
        scope: CoroutineScope,
        changeSheetType: (SheetType) -> Unit
    )
}

// Extension utilitaire (pas override ⇒ défauts autorisés)
fun IBrowser.changeState(
    isOpen: Boolean = vm.state.value.isOpen,
    item: TauItem? = vm.state.value.item,
    target: BrowserTarget? = vm.state.value.target,
    canGoBack: Boolean = vm.state.value.canGoBack,
    canGoForward: Boolean = vm.state.value.canGoForward,
    onImageClicked: (String) -> Unit = vm.state.value.onImageClicked,
) = vm.changeState(isOpen, item, target, canGoBack, canGoForward, onImageClicked)
