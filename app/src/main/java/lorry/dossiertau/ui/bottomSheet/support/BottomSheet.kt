package lorry.dossiertau.ui.bottomSheet.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import lorry.dossiertau.MainActivity
import lorry.dossiertau.SchortcutMakingState.GROUND
import lorry.dossiertau.SchortcutMakingState.ON_AIR
import lorry.dossiertau.ShortcutMakingEndMessage
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.name
import lorry.dossiertau.ui.AppBus

@Composable
fun MainActivity.BottomSheetContent(
    type: BottomSheetType,
    item: TauItem?,
    sheetText: MutableState<String>,
    isSheetVisible: MutableState<Boolean>
) {
    when (type) {
        BottomSheetType.APPLICATION -> {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .width(40.dp)
                    .background(
                        Color.LightGray,
                        RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp
                        )
                    )
            )
            {
                var text by remember { mutableStateOf("truc") }
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                ) {
                    val shortcutMakingState = remember { mutableStateOf(GROUND) }

                    LaunchedEffect(Unit) {
                        AppBus.lines.collect { line ->
                            if (shortcutMakingState.value == GROUND) {
                                shortcutMakingState.value = ON_AIR
                            } else {
                                if (line == ShortcutMakingEndMessage) {
                                    shortcutMakingState.value = GROUND
                                }
                            }
                        }
                    }

                    if (shortcutMakingState.value == GROUND)
                        Button(
                            modifier = Modifier,
                            content = { Text(text = "make HTML") },
                            onClick = {
                                viewModel.onMakeHTML(
                                    displayBottomSheet = { text ->
                                        sheetText.value = text
                                        isSheetVisible.value = true
                                    }
                                )
                            },
                        )
                    else {
                        val text = AppBus.summary.collectAsState("")

                        Text(
                            modifier = Modifier,
                            text = text.value
                        )
                    }
                }
            }
        }


        BottomSheetType.ITEM -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .width(40.dp)
                    .background(
                        Color.LightGray,
                        RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp
                        )
                    )
            )
            {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("item: ${item?.name}")
                    Button(onClick = { /* Action 1 */ }) { Text("Option 1") }
                    Button(onClick = { /* Action 2 */ }) { Text("Option 2") }
                }
            }
        }
    }
}