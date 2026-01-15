package lorry.dossiertau.ui.bottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.bottomSheet.support.BrowserTarget
import lorry.dossiertau.ui.bottomSheet.support.IBrowser

@Composable
fun ItemContent(
    item: TauItem?,
    browser: IBrowser,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.LightGray,
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
                browser = browser
            )
        }
    }
}

@Composable
fun Inside(
    item: TauItem?,
    browser: IBrowser,
) {

    val bvm = browser.vm
    val state = bvm.state.collectAsState()

    if (state.value.isOpen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp) // Hauteur fixe ou min pour la WebView dans la BottomSheet
        ) {
            browser.Render(
                modifier = Modifier.fillMaxSize()
            )
        }
    else
        Button(
            modifier = Modifier,
            onClick = {
                bvm.changeState(
                    isOpen = true,
                    item = item,
                    target = BrowserTarget.GOOGLE
                )
            }
        ) { Text(text = "Changer l'image") }
}