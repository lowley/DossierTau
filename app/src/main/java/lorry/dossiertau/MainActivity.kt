package lorry.dossiertau

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.webkit.MimeTypeMap
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
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
import java.io.File
import lorry.dossiertau.SchortcutMakingState.*

import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.ui.AppBus
import lorry.dossiertau.ui.bottomSheet.SheetVM
import lorry.dossiertau.ui.bottomSheet.Sheet
import lorry.dossiertau.ui.bottomSheet.support.SheetType
import lorry.dossiertau.ui.bottomSheet.browser.IBrowser
import lorry.dossiertau.ui.breadcrumb.BreadcrumbComponent
import lorry.dossiertau.ui.displayedItem.DisplayedItem

class MainActivity() : ComponentActivity() {

    val viewModel: TauViewModel by inject()
    val links: Links by inject()
    val breadcrumbComponent: BreadcrumbComponent by inject()

    val bsVM: SheetVM by inject()
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
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
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
                                    bsVM.changeType(SheetType.APPLICATION)
                                    scope.launch {
                                        sheetState.expand()
                                    }
                                }
                            )
                        },
                        bottomBar = {
//                            BottomAppBar(
//                                isSheetVisible = isSheetVisible,
//                                sheetText = sheetText
//                            )
                        },
//                    floatingActionButton = { /* FAB */ }
                    ) { innerPadding ->
                        val currentType = bsVM.type.collectAsState()

                        ConstraintLayout(
                            modifier = Modifier
                                .background(Color.LightGray)
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
                                    bsVM.changeType(SheetType.ITEM)
                                    sheetItem.value = item
                                    scope.launch {
                                        sheetState.expand()
                                    }
                                },
                                setAppliSheetVisible = {
                                    bsVM.changeType(SheetType.APPLICATION)
                                    scope.launch {
                                        sheetState.expand()
                                    }
                                }
                            )
                        }

                        val scope = rememberCoroutineScope()
                        val browser: IBrowser by inject()

                        if (currentType.value != null && currentType.value != SheetType.NONE) {
                            ModalBottomSheet(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 10.dp, bottom = 0.dp),
                                onDismissRequest = {
                                    browser.vm.changeState(isOpen = false)
                                    scope.launch {
                                        sheetState.hide()
                                        bsVM.changeType(SheetType.NONE)
                                    }
                                    browser.vm.changeState(target = null)
                                },
                                sheetState = sheetState,
                                containerColor = lerp(
                                    Color.LightGray,
                                    Color.DarkGray,
                                    0.2f
                                ),
                                contentWindowInsets = { WindowInsets(0) } // Pour le edge-to-edge
                            ) {
                                Sheet(
                                    modifier = Modifier
                                        .navigationBarsPadding(),
                                    type = currentType.value,
                                    item = sheetItem.value,
                                    sheetText = sheetText,
                                    sheetState = sheetState,
                                    removeSheetFromUI = {
                                        bsVM.changeType(SheetType.NONE)
                                    }
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
                window.isNavigationBarContrastEnforced = false
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
        setAppliSheetVisible: () -> Unit
    ) {
        //faire dans le ViewModel plusieurs State
        //chacun comportant plusieurs valeurs & fonctions fonctionnellement groupées
        val currentFolderPath by folderCompo.folderPathFlow.collectAsState()
        val state = rememberLazyGridState()
        val currentFolder by folderCompo.folderFlow.collectAsState()

        Box(
            modifier = modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
//                .drawWithContent {
//                    drawContent() // On dessine le contenu normalement (le carré, le texte, etc.)
//
//                    // On crée un dégradé de transparence
//                    val fadeHeight = 20.dp.toPx() // Taille de votre "zone tampon"
//
//                    drawRect(
//                        brush = Brush.verticalGradient(
//                            0f to Color.Black,             // Totalement opaque en haut de la zone
//                            1f to Color.Transparent,       // Totalement invisible tout en bas
//                            startY = size.height - fadeHeight,
//                            endY = size.height
//                        ),
//                        blendMode = BlendMode.DstIn // C'EST LA CLÉ : garde le contenu uniquement là où le dégradé est noir
//                    )
//
//                    drawRect(
//                        brush = Brush.verticalGradient(
//                            0f to Color.Black,             // Totalement opaque en haut de la zone
//                            1f to Color.Transparent,       // Totalement invisible tout en bas
//                            startY = fadeHeight,
//                            endY = 0f
//                        ),
//                        blendMode = BlendMode.DstIn // C'EST LA CLÉ : garde le contenu uniquement là où le dégradé est noir
//                    )
//                }
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
                if (currentFolder.isSome()) {
                    ItemGrid(
                        modifier = Modifier
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent() // On dessine le contenu normalement (le carré, le texte, etc.)

                                // On crée un dégradé de transparence
//                            val fadeHeight = 20.dp.toPx() // Taille de votre "zone tampon"
                                val fadeHeight =
                                    20.dp.toPx() // Augmentez un peu la zone pour mieux voir l'effet
                                val brush = Brush.verticalGradient(
                                    0.0f to Color.Black,       // 100% opaque au début de la zone tampon
                                    0.3f to Color.Black.copy(alpha = 0.5f), // Déjà à moitié transparent à 30% de la zone
                                    1.0f to Color.Transparent, // 100% invisible à la fin
                                    startY = size.height - fadeHeight,
                                    endY = size.height
                                )

                                val brush2 = Brush.verticalGradient(
                                    0.0f to Color.Black,       // 100% opaque au début de la zone tampon
                                    0.3f to Color.Black.copy(alpha = 0.5f), // Déjà à moitié transparent à 30% de la zone
                                    1.0f to Color.Transparent, // 100% invisible à la fin
                                    startY = fadeHeight,
                                    endY = 0f
                                )

                                drawRect(
                                    brush = brush,
                                    blendMode = BlendMode.DstIn // C'EST LA CLÉ : garde le contenu uniquement là où le dégradé est noir
                                )

                                drawRect(
                                    brush = brush2,
                                    blendMode = BlendMode.DstIn // C'EST LA CLÉ : garde le contenu uniquement là où le dégradé est noir
                                )
                            },
                        currentFolder = currentFolder,
                        state = state,
                        setCurrentFolder = setCurrentFolder,
                        setSheetVisible = setSheetVisible
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aucun dossier selectionné",
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { setAppliSheetVisible() }) {
                            Text("Choisir un dossier (Favoris)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { setCurrentFolder("/storage/emulated/0".toTauPath()) }) {
                            Text("Explorer le stockage interne")
                        }
                    }
                }
            else {
                ShortcutMakingLogs(lines)
            }

//            Box(
//                modifier = Modifier.height(1.dp).fillMaxWidth()
//                    .padding(start = 50.dp, end = 50.dp)
//                    .background(Color.DarkGray)
//                    .align(Alignment.BottomCenter)
//
//            )

//            Box(
//                modifier = Modifier.height(1.dp).fillMaxWidth()
//                    .padding(start = 50.dp, end = 50.dp)
//                    .background(Color.DarkGray)
//                    .align(Alignment.TopCenter)
//            )
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
        setSheetVisible: (TauItem) -> Unit,
        modifier: Modifier
    ) {
        Column(
            modifier = Modifier
        ) {
            Spacer(
                modifier = Modifier
                    .height(5.dp)
                    .background(Color.Transparent)
            )

            val allItems = currentFolder.getOrNull()!!.children

//            LaunchedEffect(allItems) {
//                if (allItems.isNotEmpty()) {
//                    state.scrollToItem(0)
//                    // Ou pour un effet plus fluide :
//                    // gridState.animateScrollToItem(0)`
//                    // state.scrollToItem(0)
//                }
//            }

            var ordering = folderCompo.ordering.collectAsState()
            val currentFolder by folderCompo.folderFlow.collectAsState()

            // On recrée un nouvel état de scroll dès que le chemin du dossier change
            // Cela garantit de repartir de zéro (en haut)
            val state = rememberLazyGridState(
                initialFirstVisibleItemIndex = 0
            )
            LazyVerticalGrid(
                modifier = modifier,
                state = state,
                columns = GridCells.Adaptive(150.dp)
//        userScrollEnabled = true,
            ) {
                items(
                    count = allItems.size,
                    //key = { index -> allItems[index].fullPath.path } // On garde la clé unique par chemin
                ) { index ->
                    val item = allItems[index]

                    DisplayedItem(
                        item = item,
                        setCurrentFolder = setCurrentFolder,
                        onClick = { filePath ->
                            Log.d("DossierTauClick", "Clic fichier: ${filePath.path}")
                            val extension = filePath.path
                                .substringAfterLast('.', "")
                                .lowercase()

                            if (extension == "html" || extension == "htm") {
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    viewModel.playingFile.playFile(
                                        filePath,
                                        "text/html",
                                        this@MainActivity
                                    )
                                }
                            } else if (!openWithAndroidDefaultApp(filePath)) {
                                if (extension in listOf("avi", "mp4", "mkv", "ts", "mpg", "mpeg")) {
                                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                                        viewModel.playingFile.playFile(
                                            filePath,
                                            "video/mp4",
                                            this@MainActivity
                                        )
                                    }
                                }
                            }
                        },
                        onLongClick = { item ->
                            viewModel.setSelectedItem(item)
                            setSheetVisible(item)
                        },
                    )
                }
            }
        }
    }

    private fun openWithAndroidDefaultApp(filePath: TauPath): Boolean {
        val file = File(filePath.path)
        if (!file.exists() || !file.isFile) return false

        val extension = file.extension.lowercase()
        val mimeType = when (extension) {
            "m3u8" -> "application/vnd.apple.mpegurl"
            "m3u" -> "audio/x-mpegurl"
            else -> MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension)
                ?: "*/*"
        }

        val uri = runCatching {
            FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file,
            )
        }.getOrNull() ?: return false

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val resolved = packageManager.resolveActivity(
            intent,
            MATCH_DEFAULT_ONLY,
        ) ?: return false

        // Si Android renvoie son ResolverActivity, aucune application
        // n'est réellement définie par défaut pour ce type de fichier.
        if (resolved.activityInfo.packageName == "android") return false

        return runCatching {
            startActivity(intent)
            true
        }.getOrDefault(false)
    }

    @Composable
    private fun TopAppBar(setSheetVisible: () -> Unit) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(55.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.maison),
                contentDescription = "Accueil / Favoris",
                modifier = Modifier
                    .padding(start = 15.dp)
                    .size(28.dp)
                    .align(Alignment.CenterVertically)
                    .clickable {
                        setSheetVisible()
                    },
                tint = Color.DarkGray
            )

            //faire dans le ViewModel plusieurs State
            //chacun comportant plusieurs valeurs & fonctions fonctionnellement groupées
            val currentFolderItems by folderCompo.folderPathFlow
                .map {
                    it.getOrNull()?.path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
                }
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

            var ordering = folderCompo.ordering.collectAsState()

            Spacer(
                modifier = Modifier.weight(10f))

            Icon(
                painter = painterResource(id = R.drawable.pluma_0),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.CenterVertically)
                    .padding(end = 10.dp)
                    .clickable {
                        folderCompo.setOrdering(true)
                        folderCompo.setFolderOrdering(ordering.value)
                    },
                tint = if (!ordering.value) Color.DarkGray else Color.Unspecified
            )

            Icon(
                painter = painterResource(id = R.drawable.sortbydate),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.CenterVertically)
                    .padding(end = 10.dp)
                    .clickable {
                        folderCompo.setOrdering(true)
                        folderCompo.setFolderOrdering(ordering.value)
                    },
                tint = if (!ordering.value) Color.DarkGray else Color.Unspecified
            )

            Icon(
                painter = painterResource(id = R.drawable.sortbyalpha2),
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .align(Alignment.CenterVertically)
                    .padding(end = 10.dp)
                    .clickable {
                        folderCompo.setOrdering(false)
                        folderCompo.setFolderOrdering(ordering.value)
                    },
                tint = if (ordering.value) Color.DarkGray else Color.Unspecified

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

const val ShortcutMakingEndMessage = "EndMessage"