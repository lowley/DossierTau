package lorry.dossiertau

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.raise.fold
import lorry.dossiertau.data.model.children
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.name
import lorry.dossiertau.support.littleClasses.EMPTY
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.ui.theme.DossierTauTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    val viewModel: TauViewModel by inject()
    val folderCompo = viewModel.folderCompo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        val permissionsManager = PermissionsManager()
        if (!permissionsManager.hasExternalStoragePermission())
            permissionsManager.requestExternalStoragePermission(this)

        setContent {
            DossierTauTheme {
                Scaffold(
                    modifier = Modifier,
                    topBar = { TopAppBar() },
                    bottomBar = { BottomAppBar() },
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
                                .width(30.dp)
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
                                    height = Dimension.matchParent
                                    width = Dimension.fillToConstraints
                                },
                            setCurrentFolder = { newFolder: TauPath ->
                                viewModel.setTauFolder(newFolder)
                            }
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
            if (currentFolder.isSome()) {
                LazyVerticalGrid(
                    modifier = Modifier,
                    state = state,
                    columns = GridCells.Adaptive(175.dp)
//        userScrollEnabled = true,
                ) {
                    items(currentFolder.getOrNull()!!.children.size) { index ->
                        val item = currentFolder.getOrNull()!!.children[index]

                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .border(1.dp, Color.DarkGray, shape = RoundedCornerShape(8.dp))
                                .clickable{
                                    if (item.isFolder())
                                        setCurrentFolder(item.fullPath)
                                }
                        ) {
                            Text(
                                text = item.name.value,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray)
                            )
                        }
                    }
                }
            }
            else {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center),
                    text = "Aucun dossier selectionné"
                )

            }
        }
    }

    @Composable
    private fun TopAppBar() {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(55.dp)
        ) {
            //faire dans le ViewModel plusieurs State
            //chacun comportant plusieurs valeurs & fonctions fonctionnellement groupées
            val currentFolderPath by folderCompo.folderPathFlow.collectAsState()

            currentFoldePathText(
                modifier = Modifier
                    .fillMaxSize(),
                currentFolder = currentFolderPath,
                setCurrentFolder = { newFolder: TauPath ->
                    folderCompo.setFolderFlow(newFolder)
                }
            )
        }

    }

    @Composable
    private fun BottomAppBar() {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(35.dp)
                .background(Color.LightGray)
        ) {

        }
    }

}

@Composable
fun currentFoldePathText(
    modifier: Modifier,
    currentFolder: Option<TauPath>,
    setCurrentFolder: (TauPath) -> Unit,


    ) {

    TextField(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        value = when (currentFolder) {
            is Some<TauPath> -> when (currentFolder.value.value) {
                is Either.Left -> "<aucun chemin sélectionné>"
                is Either.Right -> (currentFolder.value.value as Either.Right<TauPath.Companion.Data>).value.value
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






