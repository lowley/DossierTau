package lorry.dossiertau.ui.bottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lorry.dossiertau.SchortcutMakingState
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.ui.AppBus
import lorry.dossiertau.usecases.applicationFavorites.AppliFavos
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationContent(
    viewModel: TauViewModel,
    sheetText: MutableState<String>,
    shortcutMakingEndMessage: String,
    sheetState: SheetState,
    modifier: Modifier
) {
    val appliFavos: AppliFavos by inject(AppliFavos::class.java)
    val favoris by appliFavos.appliFavorites.collectAsState()
    val folderCompo: IFolderCompo by inject(IFolderCompo::class.java)

    var text by remember { mutableStateOf("truc") }
    Column(
        modifier = modifier
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            favoris.forEach { favori ->
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(lerp(
                            Color.DarkGray,
                            Color.LightGray,
                            0.6f
                        ))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))

                ){
                    val scope = rememberCoroutineScope()

                    AsyncImage(
                        modifier = Modifier
                            .size(72.dp)
                            .padding(top = 8.dp)
                            .clickable{
                                scope.launch {
                                    sheetState.hide()
                                }

                                folderCompo.setFolderFlow(favori.fullPath)
                            },
                        model = favori.picture.toBitmap(),
                        contentDescription = "image du favori",
//                        colorFilter = ColorFilter.tint(Color(0xFFE1D693))
                    )

                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        text = favori.name.value.take(25).trim(),
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                    )
                }
            }
        }

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
                                sheetState.expand()
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
