package lorry.dossiertau.ui.bottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import lorry.dossiertau.MainActivity
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.name
import lorry.dossiertau.ui.bottomSheet.support.BrowserTarget
import lorry.dossiertau.ui.bottomSheet.support.GestureOwner
import lorry.dossiertau.ui.bottomSheet.support.IBrowser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.ItemContent(
    item: TauItem?,
    browser: IBrowser,
    sheetState: SheetState,
    tauvm: TauViewModel,
    gestureOwner: MutableState<GestureOwner>,
) {
    Box(
        modifier = Modifier
            .height(650.dp)
            .fillMaxWidth()
            .background(
                Color.DarkGray,
                RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp
                )
            )
    )
    {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Inside(
                item = item,
                browser = browser,
                sheetState = sheetState,
                tauvm = tauvm,
                gestureOwner = gestureOwner
            )

            // Petit espacement en bas pour éviter que le contenu soit collé au bord
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(Color.DarkGray)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.Inside(
    item: TauItem?,
    browser: IBrowser,
    sheetState: SheetState,
    tauvm: TauViewModel,
    gestureOwner: MutableState<GestureOwner>,
) {
    val bvm = browser.vm
    val state = bvm.state.collectAsState()
    val scope = rememberCoroutineScope()

    if (state.value.isOpen)
        Column(
            modifier = Modifier
        ) {
            browser.Render(
                modifier = Modifier.heightIn(max = 600.dp),
                gestureOwner = gestureOwner,
            )
        }
    else
        Button(
            modifier = Modifier,
            onClick = {
                bvm.changeState(
                    isOpen = true,
                    item = item,
                    target = BrowserTarget.GOOGLE.withQuery(item?.name?.value ?: ""),
                    onImageClicked = { imageUrl ->
                        browser.manageImageClick(
                            viewModel = tauvm,
                            imageUrl = imageUrl,
                            sheetState = sheetState,
                            scope = scope
                        )
                    }
                )
                scope.launch {
//                    sheetState.hide()
                    sheetState.expand()
                }
            }
        ) { Text(text = "Changer l'image") }
}