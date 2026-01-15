package lorry.dossiertau.ui.bottomSheet.support

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import lorry.dossiertau.TauApp
import lorry.dossiertau.TauViewModel
import org.koin.java.KoinJavaComponent.inject
import androidx.lifecycle.AndroidViewModel
class Browser(
) : IBrowser {

    override val vm: BrowserViewModel by inject(BrowserViewModel::class.java)
//    override val vm: BrowserViewModel by lazy {
//        val activity = context.findActivity()
//        ViewModelProvider(activity)[BrowserViewModel::class.java]
//    }

    @Composable
    fun rememberActivityContext(): Activity? {
        val context = LocalContext.current
        return remember(context) {
            context.findActivity()
        }
    }

    @Composable
    override fun rememberBrowserState(): BrowserState {
        return remember { BrowserState() }
    }

    ////////////
    // zoneUI //
    ////////////
    @Composable
    override fun Render(modifier: Modifier) {
        val browserState: BrowserState by vm.state.collectAsState()

        if (browserState.isOpen)
            BrowserWindow(
                modifier = modifier,
                browserState = browserState,
                onImageClicked = { imageUrl ->
                    browserState.onImageClicked(imageUrl)
                },
                setCanGoBack = { value ->
                    vm.changeState(
                        canGoBack = value
                    )
                },
                setCanGoForward = { value ->
                    vm.changeState(
                        canGoForward = value
                    )
                },
                closeBrowser = {
                    vm.close()
                }
            )
    }

    private fun Context.findActivity(): ComponentActivity =
        generateSequence(this) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<ComponentActivity>()
            .firstOrNull()
            ?: error("Browser attend un @ActivityContext ; vérifie le scope et l’annotation.")
}

fun manageImageClick(viewModel: TauViewModel, imageUrl: String) {
//    if (viewModel.selectedItem.value != null)
//        viewModel.viewModelScope.launch {
//            viewModel.updatePicture(imageUrl)
//            SigmaViewModel.requestRefresh()
//
//        }
}

