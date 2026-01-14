package lorry.dossiertau

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.VerticalAlign
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import coil.compose.AsyncImage
import com.mutkuensert.basicbottomsheet.BasicBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.CIA
import lorry.dossiertau.data.model.children
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.isFile
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.name
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.ui.theme.DossierTauTheme
import lorry.dossiertau.usecases.generateHTMLs.Links
import org.koin.android.ext.android.inject
import lorry.dossiertau.SchortcutMakingState.*

import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.picture
import lorry.dossiertau.ui.AppBus
import lorry.dossiertau.ui.breadcrumb.BreadcrumbComponent
import lorry.dossiertau.ui.displayedItem.DisplayedItem

class MainActivity() : ComponentActivity() {

    val viewModel: TauViewModel by inject()
    val links: Links by inject()
    val breadcrumbComponent: BreadcrumbComponent by inject()

    val folderCompo = viewModel.folderCompo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        val permissionsManager = PermissionsManager()
        if (!permissionsManager.hasExternalStoragePermission())
            permissionsManager.requestExternalStoragePermission(this)

        startSpying()

        setContent {
            DossierTauTheme {
                val isSheetVisible = remember { mutableStateOf(false) }
                val sheetText = remember { mutableStateOf("") }

                Scaffold(
                    modifier = Modifier,
                    topBar = { TopAppBar() },
                    bottomBar = {
                        BottomAppBar(
                            isSheetVisible = isSheetVisible,
                            sheetText = sheetText
                        )
                    },
//                    floatingActionButton = { /* FAB */ }
                ) { innerPadding ->

                    ConstraintLayout(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        val (leftPanel, content, statusBar) = createRefs()

                        LeftPane(
                            Modifier
                                .width(20.dp)
                                .fillMaxHeight()
                                .constrainAs(leftPanel) {
                                    start.linkTo(parent.start)
                                }
                        )

                        StatusBar(
                            Modifier
                                .height(45.dp)
                                .fillMaxWidth()
                                .constrainAs(statusBar) {
                                    bottom.linkTo(parent.bottom)
                                }
//                                .background(Color.LightGray)
                        )

                        MainPage(
                            Modifier
                                .constrainAs(content) {
                                    start.linkTo(leftPanel.end)
                                    end.linkTo(parent.end)
                                    bottom.linkTo(parent.bottom)
                                    height = Dimension.matchParent
                                    width = Dimension.fillToConstraints
                                },
                            setCurrentFolder = { newFolder: TauPath ->
                                viewModel.setTauFolder(newFolder)
                            }
                        )
                    }

                    BasicBottomSheet(
                        visible = isSheetVisible.value,
                        onCloseSheet = { isSheetVisible.value = false },
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(16.dp)
                                .padding(5.dp),
                            text = sheetText.value,
                            color = Color.Black,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LeftPane(
        constrainAs: Modifier
    ) {
        Box(
            modifier = constrainAs
        ) {

        }
    }

    @Composable
    private fun StatusBar(modifier: Modifier) {

    }

    @Composable
    fun MainPage(
        modifier: Modifier = Modifier,
        setCurrentFolder: (TauPath) -> Unit,
    ) {

        //faire dans le ViewModel plusieurs State
        //chacun comportant plusieurs valeurs & fonctions fonctionnellement groupées
        val currentFolderPath by folderCompo.folderPathFlow.collectAsState()
        val state = rememberLazyGridState()
        val currentFolder by folderCompo.folderFlow.collectAsState()

        Box(
            modifier = modifier
        )
        {
            val lines = remember { mutableStateOf<List<String>>(emptyList()) }
            val shortcutMakingState = remember { mutableStateOf(GROUND) }

            LaunchedEffect(Unit) {
                AppBus.lines.collect { line ->
                    if (shortcutMakingState.value == GROUND) {
                        shortcutMakingState.value = ON_AIR
                    } else {
                        if (line == ShortcutMakingEndMessage) {
                            shortcutMakingState.value = GROUND
                            lines.value = emptyList<String>()
                        } else
                            lines.value = lines.value.plus(line)
                    }
                }
            }

            if (shortcutMakingState.value == GROUND)
                ItemGrid(
                    currentFolder = currentFolder,
                    state = state,
                    setCurrentFolder = setCurrentFolder
                )
            else {
                ShortcutMakingLogs(lines)
            }
        }
    }

    context(BoxScope)
    @Composable
    private fun ShortcutMakingLogs(lines: MutableState<List<String>>) {
        LazyColumn {
            items(lines.value.size) { index ->
                val line = lines.value[index]
                val notoFont = FontFamily(Font(R.font.segoe_regular))

                Text(
                    text = line,
                    fontFamily = notoFont,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }

    context(BoxScope)
    @Composable
    fun ItemGrid(
        currentFolder: Option<TauFolder>,
        state: LazyGridState,
        setCurrentFolder: (TauPath) -> Unit
    ) {
        if (currentFolder.isSome()) {
            LazyVerticalGrid(
                modifier = Modifier,
                state = state,
                columns = GridCells.Adaptive(150.dp)
//        userScrollEnabled = true,
            ) {
                items(currentFolder.getOrNull()!!.children.size) { index ->
                    val item = currentFolder.getOrNull()!!.children
                        .sortedBy { it.isFile().toString() + it.name }[index]

                    key(item.fullPath) {
                        DisplayedItem(
                            item = item,
                            setCurrentFolder = setCurrentFolder,
                            onClick = { filePath ->
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    if (filePath.path.endsWith("html"))
                                        viewModel.playingFile.playFile(
                                            filePath,
                                            "text/html",
                                            this@MainActivity
                                        )
                                    else
                                        viewModel.playingFile.playFile(
                                            filePath,
                                            "video/mp4",
                                            this@MainActivity
                                        )
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = "Aucun dossier selectionné"
            )
        }
    }

    @Composable
    private fun TopAppBar() {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(55.dp)
        ) {
            //faire dans le ViewModel plusieurs State
            //chacun comportant plusieurs valeurs & fonctions fonctionnellement groupées
            val currentFolderItems by folderCompo.folderPathFlow
                .map { it.getOrNull()?.path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList() }
                .collectAsState(emptyList())

            if (currentFolderItems.isNotEmpty())
                breadcrumbComponent.Breadcrumb(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 20.dp),
                    path = currentFolderItems,
                    onClick = {
                        folderCompo.setFolderFlow(it)
                    }
                )
        }

    }

    @Composable
    private fun BottomAppBar(
        isSheetVisible: MutableState<Boolean>,
        sheetText: MutableState<String>
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(35.dp)
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

    private fun startSpying() {
        val intent = Intent(this, CIA::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun currentFolderPathText(
    modifier: Modifier,
    optionCurrentFolder: Option<TauPath>,
    setCurrentFolder: (TauPath) -> Unit,
) {

    TextField(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        value = when (optionCurrentFolder) {
            is Some<TauPath> -> {
                val result = when (val path = optionCurrentFolder.value.path) {
                    "" -> "<aucun chemin sélectionné>"
                    else -> path
                }

                result
            }

            is None -> "<aucun chemin sélectionné>"
        },
        onValueChange = {
            setCurrentFolder(it.toTauPath())
        },
        trailingIcon = {

        }
    )
}

enum class SchortcutMakingState {
    ON_AIR,
    GROUND
}

val ShortcutMakingEndMessage = "EndMessage"




