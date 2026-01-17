package lorry.dossiertau.ui.bottomSheet.support

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauItem

interface IBrowser {

    val vm: BrowserViewModel

    @Composable
    fun rememberBrowserState(): BrowserState
    @Composable
    fun Render(
        modifier: Modifier,
        gestureOwner: MutableState<GestureOwner>)

    @OptIn(ExperimentalMaterial3Api::class)
    fun manageImageClick(
        viewModel: TauViewModel,
        imageUrl: String,
        sheetState: SheetState,
        scope: CoroutineScope
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
