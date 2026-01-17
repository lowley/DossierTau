package lorry.dossiertau.ui.bottomSheet.support

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import lorry.dossiertau.TauViewModel
import org.koin.java.KoinJavaComponent.inject
import arrow.core.toOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.copy
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.ui.support.base64.IVideoInfoEmbedder
import lorry.dossiertau.ui.support.capsule.CapsuleComponent
import lorry.dossiertau.ui.support.capsule.utilities.CroppedPicture
import lorry.dossiertau.ui.support.capsule.utilities.InitialPicture
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import kotlin.getValue

class Browser(
) : IBrowser {

    override val vm: BrowserViewModel by inject(BrowserViewModel::class.java)
//    override val vm: BrowserViewModel by lazy {
//        val activity = context.findActivity()
//        ViewModelProvider(activity)[BrowserViewModel::class.java]
//    }

    val folderCompo: IFolderCompo by inject(IFolderCompo::class.java)


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
    override fun Render(modifier: Modifier, gestureOwner: MutableState<GestureOwner>) {
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
                },
                gestureOwner = gestureOwner
            )
    }

    private fun Context.findActivity(): ComponentActivity =
        generateSequence(this) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<ComponentActivity>()
            .firstOrNull()
            ?: error("Browser attend un @ActivityContext ; vérifie le scope et l’annotation.")

    override fun manageImageClick(viewModel: TauViewModel, imageUrl: String) {

        val selectedItem = viewModel.selectedItem.value ?: return
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            doWorkWhenImageClicked(
                imageUrl, selectedItem
            )
        }
    }

    context(CoroutineScope)
    private suspend fun doWorkWhenImageClicked(
        imageUrl: String,
        selectedItem: TauItem
    ) {
        val image = async {
            vm.urlToBitmap(imageUrl)
        }.await() ?: return

        val currentFolder = folderCompo.folderFlow.value.getOrNull()
        val imageBitmap = TauPicture.fromBitmap(image)
        val newFolder = currentFolder?.modifyItem(
            selectedItem.copy(picture = imageBitmap)
        )

        if (newFolder != null)
            folderCompo.changeFolderFlow(newFolder.toOption())

        val capsuleMgr = CapsuleComponent()
//            val capsule = capsuleMgr.getCapsule(selectedItem.fullPath)
        capsuleMgr.save(
            element = InitialPicture(imageBitmap.bitmap, lorry.dossiertau.ui.support.base64.VideoInfoEmbedder() as IVideoInfoEmbedder),
            targetPath = selectedItem.fullPath,
            useOld = false
        )

        capsuleMgr.save(
            element = CroppedPicture(imageBitmap.bitmap, lorry.dossiertau.ui.support.base64.VideoInfoEmbedder() as IVideoInfoEmbedder),
            targetPath = selectedItem.fullPath,
            useOld = false
        )
    }
}



