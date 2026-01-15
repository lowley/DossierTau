package lorry.dossiertau

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.mutkuensert.basicbottomsheet.BasicBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.CIA
import lorry.dossiertau.data.model.children
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.isFile
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
import lorry.dossiertau.ui.AppBus
import lorry.dossiertau.ui.bottomSheet.support.BottomSheetContent
import lorry.dossiertau.ui.bottomSheet.support.BottomSheetType
import lorry.dossiertau.ui.breadcrumb.BreadcrumbComponent
import lorry.dossiertau.ui.displayedItem.DisplayedItem

class MainActivity() : ComponentActivity() {

    val viewModel: TauViewModel by inject()
    val links: Links by inject()
    val breadcrumbComponent: BreadcrumbComponent by inject()

    val folderCompo = viewModel.folderCompo

    @OptIn(ExperimentalMaterial3Api::class)
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
                var currentType = remember { mutableStateOf<BottomSheetType?>(null) }
                val sheetState = rememberModalBottomSheetState()
                val sheetItem = remember { mutableStateOf<TauItem?>(null) }
                val scope = rememberCoroutineScope()

                val sheetText = remember { mutableStateOf("") }

                SetBlackBackgroundForNavigationBar(Color.Transparent)

                NoRippleThemeContent {
                    Scaffold(
                        modifier = Modifier,
                        topBar = {
                            TopAppBar(
                                setSheetVisible = {
                                    currentType.value = BottomSheetType.APPLICATION
                                    isSheetVisible.value = true
                                }
                            )
                        },
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
                                .background(Color.Black)
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
                                },
                                setSheetVisible = { item ->
                                    currentType.value = BottomSheetType.ITEM
                                    sheetItem.value = item
                                    isSheetVisible.value = true
                                }
                            )
                        }

                        if (isSheetVisible.value && currentType.value != null) {
                            ModalBottomSheet(
                                onDismissRequest = { isSheetVisible.value = false; currentType.value = null },
                                sheetState = sheetState,
                                // ✅ Bord intégré au container (pas de décalage)
                                containerColor = Color(0xFF333333),
                                tonalElevation = 0.dp,  // Supprime l'ombre qui décale
                                // ✅ Pas de shape qui clippe le border
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                dragHandle = {
                                    // ✅ Handle custom parfaitement centré
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp, bottom = 12.dp)
//                                            .border(
//                                                width = 2.dp,
//                                                color = Color.Blue,
//                                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)  // Même shape !
//                                            )// Padding uniforme
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .width(40.dp)
                                                .height(4.dp)
                                                .background(
                                                    Color.Red,
                                                    CircleShape  // Rond parfait
                                                )
                                        )
                                    }
                                }
                            ) {
                                BottomSheetContent(
                                    currentType.value!!,
                                    item = sheetItem.value
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SetBlackBackgroundForNavigationBar(
        color: Color
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = color.toArgb()  // Barre nav NOIRE
            window.statusBarColor = color.toArgb()      // Bonus : status bar noire

            // Icônes blanches sur fond noir
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightNavigationBars = false
                isAppearanceLightStatusBars = false
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
        setSheetVisible: (TauItem) -> Unit,
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
                    setCurrentFolder = setCurrentFolder,
                    setSheetVisible = setSheetVisible
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
        setCurrentFolder: (TauPath) -> Unit,
        setSheetVisible: (TauItem) -> Unit
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
                            },
                            onLongClick = { item ->
                                setSheetVisible(item)
                            },
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
    private fun TopAppBar(setSheetVisible: () -> Unit) {
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
                    },
                    onArrowClicked = {
                        setSheetVisible()
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
        startForegroundService(intent)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoRippleThemeContent(content: @Composable () -> Unit) {
    val noRippleConfig = remember {
        RippleConfiguration(
            color = Color.Unspecified,
            rippleAlpha = RippleAlpha(
                pressedAlpha = 0f,
                draggedAlpha = 0f,
                focusedAlpha = 0f,
                hoveredAlpha = 0f
            )
        )
    }

    CompositionLocalProvider(LocalRippleConfiguration provides noRippleConfig) {
        content()
    }
}