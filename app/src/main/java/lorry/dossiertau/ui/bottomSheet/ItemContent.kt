package lorry.dossiertau.ui.bottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetState
import kotlinx.coroutines.launch
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.support.BrowserTarget
import lorry.dossiertau.ui.bottomSheet.support.IBrowser

@Composable
fun ItemContent(
    item: TauItem?,
    browser: IBrowser,
    sheetState: FlexibleSheetState,
    tauvm: TauViewModel,
) {
    Box(
        modifier = Modifier
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
            modifier = Modifier
        ) {

            Inside(
                item = item,
                browser = browser,
                sheetState = sheetState,
                tauvm = tauvm
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.DarkGray)
            )
        }
    }
}

@Composable
fun Inside(
    item: TauItem?,
    browser: IBrowser,
    sheetState: FlexibleSheetState,
    tauvm: TauViewModel,
) {
    val bvm = browser.vm
    val state = bvm.state.collectAsState()
    val scope = rememberCoroutineScope()

    if (state.value.isOpen)
        browser.Render(
            modifier = Modifier.fillMaxSize()
        )
    else
        Button(
            modifier = Modifier,
            onClick = {
                bvm.changeState(
                    isOpen = true,
                    item = item,
                    target = BrowserTarget.GOOGLE,
                    onImageClicked = { imageUrl ->
                        browser.manageImageClick(
                            viewModel = tauvm,
                            imageUrl = imageUrl
                        )
                    }
                )
                scope.launch {
//                    sheetState.hide()
                    sheetState.fullyExpand()
                }
            }
        ) { Text(text = "Changer l'image") }
}