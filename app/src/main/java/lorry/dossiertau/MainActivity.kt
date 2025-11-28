package lorry.dossiertau

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                            .background(Color.DarkGray)
                            .border(1.dp, Color.White)
                    ) {
//                        val left = createRef()
//                        val (left, right) = createRefs()
                        val (leftPanel, content, rightPanel, statusBar) = createRefs()

//                        Box(
//                            modifier = Modifier
//                                .width(30.dp)
//                                .fillMaxHeight()
////                                .size(100.dp)
//                                .background(Color.Red)
//                                .constrainAs(left) { // Reference boxA added to this box.
//                                    top.linkTo(parent.top) // link top of boxA to top of parent.
//                                    bottom.linkTo(parent.bottom)
//                                    start.linkTo(parent.start) // link start of boxA to start of parent.
//                                },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text("A", fontSize = 30.sp)
//                        }


//                        Box(
//                            modifier = Modifier
//                                .width(30.dp)
//                                .fillMaxHeight()
//                                .constrainAs(left) {
//                                    top.linkTo(parent.top)
//                                    bottom.linkTo(parent.bottom)
//                                    start.linkTo(parent.start)
////                                    end.linkTo(right.start)
////                                    height = Dimension.fillToConstraints
////                                    height = Dimension.fillToConstraints
//                                }
//                                .background(Color.Red)
//                                .border(1.dp, Color.Black),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text("A", fontSize = 30.sp)
//                        }
//
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .constrainAs(right) {
//                                    start.linkTo(left.end)
//                                    top.linkTo(parent.top)
////                                    bottom.linkTo(parent.bottom)
////                                    end.linkTo(parent.end)
//                                }
//                                .background(Color.Blue)
//                                .border(1.dp, Color.Black),
//                            contentAlignment = Alignment.Center
//                        ) {
//
//                            Text("A", fontSize = 30.sp)
//                        }

                        LeftPane(
                            Modifier
                                .width(30.dp)
                                .fillMaxHeight()
                                .constrainAs(leftPanel) {
//                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
//                                    bottom.linkTo(statusBar.top)
//                                    width = Dimension.value(280.dp)
//                                    height = Dimension.fillToConstraints
                                }
                                .background(Color.Blue)
                                .border(3.dp, Color.Black)
                        )

                        RightPane(
                            Modifier
                                .width(30.dp)
                                .fillMaxHeight()
                                .constrainAs(rightPanel) {
//                                    top.linkTo(parent.top)
//                                    bottom.linkTo(status.top)
                                    end.linkTo(parent.end)
//                                    width = Dimension.value(320.dp)
//                                    height = Dimension.fillToConstraints
                                }
                                .background(Color.Green)
                                .border(3.dp, Color.Black)
                        )

                        StatusBar(
                            Modifier
                                .height(45.dp)
                                .fillMaxWidth()
                                .constrainAs(statusBar) {
//                                    start.linkTo(parent.start)
//                                    end.linkTo(parent.end)
                                    bottom.linkTo(parent.bottom)
//                                    height = Dimension.value(45.dp)
//                                    width = Dimension.fillToConstraints
                                }
                                .background(Color.LightGray)
                        )

                        MainPage(
                            Modifier
                                .constrainAs(content) {
                                    start.linkTo(leftPanel.end)
//                                    top.linkTo(leftPanel.top)
                                    end.linkTo(rightPanel.start)
//                                    bottom.linkTo(statusBar.top)
//                                    width = Dimension.percent(1f)
//                                    height = Dimension.percent(1f)
                                    height = Dimension.matchParent
                                    width = Dimension.fillToConstraints
                                }
                                .background(Color.Yellow)
                                .border(2.dp, Color.Black)
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
    private fun RightPane(constrainAs: Modifier) {
        Box(
            modifier = constrainAs
        ) {

        }
    }

    @Composable
    private fun StatusBar(modifier: Modifier) {
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
            val currentFolder by viewModel.currentFoldePath.collectAsState()

            currentFoldePathText(
                modifier = Modifier
                    .fillMaxSize(),
                currentFolder = currentFolder,
                setCurrentFolder = { newFolder: TauPath ->
                    viewModel.setCurrentFoldePath(newFolder)
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
                .background(Color.Magenta)
        ) {

        }
    }

}

@Composable
fun MainActivity.MainPage(modifier: Modifier = Modifier) {

    //faire dans le ViewModel plusieurs State
    //chacun comportant plusieurs valeurs & fonctions fonctionnellement groupées
    val currentFolderPath by viewModel.currentFoldePath.collectAsState()
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
fun currentFoldePathText(
    modifier: Modifier,
    currentFolder: TauPath,
    setCurrentFolder: (TauPath) -> Unit,


    ) {

    TextField(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        value = when (currentFolder) {
            is TauPath.EMPTY -> "<aucun chemin sélectionné>"
            is TauPath.Data -> currentFolder.value
        },
        onValueChange = {
            setCurrentFolder(it.toTauPath())
        },
        trailingIcon = {

        }
    )
}






