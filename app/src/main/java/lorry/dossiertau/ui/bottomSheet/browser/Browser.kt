package lorry.dossiertau.ui.bottomSheet.browser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import lorry.dossiertau.TauViewModel
import org.koin.java.KoinJavaComponent.inject
import arrow.core.toOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.copy
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.ui.bottomSheet.browser.support.BrowserState
import lorry.dossiertau.ui.bottomSheet.browser.support.BrowserWindow
import lorry.dossiertau.ui.bottomSheet.support.SheetType
import lorry.dossiertau.ui.support.base64.IVideoInfoEmbedder
import lorry.dossiertau.ui.support.base64.VideoInfoEmbedder
import lorry.dossiertau.ui.support.capsule.CapsuleComponent
import lorry.dossiertau.ui.support.capsule.utilities.CroppedPicture
import lorry.dossiertau.ui.support.capsule.utilities.InitialPicture
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import kotlin.getValue

class Browser(
) : IBrowser {

    override val vm: BrowserVM by inject(BrowserVM::class.java)

    val folderCompo: IFolderCompo by inject(IFolderCompo::class.java)

    @Composable
    fun rememberActivityContext(): Activity {
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
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        exitImageSelection: () -> Unit
    ) {
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
                exitImageSelection = exitImageSelection
            )
    }

    private fun Context.findActivity(): ComponentActivity =
        generateSequence(this) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<ComponentActivity>()
            .firstOrNull()
            ?: error("Browser attend un @ActivityContext ; vérifie le scope et l’annotation.")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun manageImageClick(
        viewModel: TauViewModel,
        imageUrl: String,
        sheetState: SheetState,
        scope: CoroutineScope,
        changeSheetType: (SheetType) -> Unit
    ) {
        val selectedItem = viewModel.selectedItem.value ?: return

        scope.launch {
            doWorkWhenImageClicked(
                imageUrl = imageUrl,
                selectedItem = selectedItem,
                sheetState = sheetState,
                scope = scope,
                changeSheetType = changeSheetType

            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private suspend fun doWorkWhenImageClicked(
        imageUrl: String,
        selectedItem: TauItem,
        sheetState: SheetState,
        scope: CoroutineScope,
        changeSheetType: (SheetType) -> Unit = {}
    ) {
        scope.launch(Dispatchers.Main) {
            sheetState.hide()
            changeSheetType(SheetType.NONE)
        }

        val image = withContext(Dispatchers.IO) {
            vm.urlToBitmap(imageUrl)
        }

        if (image == null)
            return

        val currentFolder = folderCompo.folderFlow.value.getOrNull()
        val imageBitmap = TauPicture.fromBitmap(image)
        val newFolder = currentFolder?.modifyItem(
            selectedItem.copy(picture = imageBitmap)
        )

        if (newFolder != null)
            scope.launch(Dispatchers.Main) {
                folderCompo.changeFolderFlow(newFolder.toOption())
            }

        val capsuleMgr = CapsuleComponent()
//            val capsule = capsuleMgr.getCapsule(selectedItem.fullPath)

        capsuleMgr.save(
            element = InitialPicture(
                imageBitmap.bitmap,
                VideoInfoEmbedder() as IVideoInfoEmbedder
            ),
            targetPath = selectedItem.fullPath,
            useOld = false
        )

        capsuleMgr.save(
            element = CroppedPicture(
                imageBitmap.bitmap,
                VideoInfoEmbedder() as IVideoInfoEmbedder
            ),
            targetPath = selectedItem.fullPath,
            useOld = false
        )
    }
}



