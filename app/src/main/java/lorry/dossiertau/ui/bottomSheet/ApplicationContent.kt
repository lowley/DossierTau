package lorry.dossiertau.ui.bottomSheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lorry.dossiertau.SchortcutMakingState
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.ui.AppBus

@Composable
fun ApplicationContent(
    viewModel: TauViewModel,
    sheetText: MutableState<String>,
    shortcutMakingEndMessage: String,
    sheetState: FlexibleSheetState
) {
    var text by remember { mutableStateOf("truc") }
    Column(
        modifier = Modifier.Companion
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
    ) {
        val shortcutMakingState =
            remember { mutableStateOf(SchortcutMakingState.GROUND) }

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        LaunchedEffect(Unit) {
            AppBus.lines.collect { line ->
                if (shortcutMakingState.value == SchortcutMakingState.GROUND) {
                    shortcutMakingState.value = SchortcutMakingState.ON_AIR
                } else {
                    if (line == shortcutMakingEndMessage) {
                        shortcutMakingState.value = SchortcutMakingState.GROUND
                    }
                }
            }
        }

        if (shortcutMakingState.value == SchortcutMakingState.GROUND)
            Button(
                modifier = Modifier.Companion,
                content = { Text(text = "make HTML") },
                onClick = {
                    viewModel.onMakeHTML(
                        displayBottomSheet = { text ->
                            sheetText.value = text
                            scope.launch{
                                sheetState.fullyExpand()
                            }
                        }
                    )
                },
            )
        else {
            val text = AppBus.summary.collectAsState("")

            Text(
                modifier = Modifier.Companion,
                text = text.value
            )
        }
    }
}
