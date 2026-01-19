package lorry.dossiertau.ui.bottomSheet.contents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lorry.dossiertau.MainActivity
import lorry.dossiertau.R
import lorry.dossiertau.TauColors
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.isFile
import lorry.dossiertau.data.model.modificationDate
import lorry.dossiertau.data.model.name
import lorry.dossiertau.ui.bottomSheet.support.SheetType
import lorry.dossiertau.ui.bottomSheet.browser.support.BrowserTarget
import lorry.dossiertau.ui.bottomSheet.browser.BrowserVM
import lorry.dossiertau.ui.bottomSheet.browser.IBrowser
import lorry.dossiertau.usecases.applicationFavorites.AppliFavos
import lorry.dossiertau.usecases.applicationFavorites.contains
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.ContentItem(
    item: TauItem?,
    browser: IBrowser,
    sheetState: SheetState,
    tauvm: TauViewModel,
    modifier: Modifier,
    changeSheetType: (SheetType) -> Unit = {}

) {
    Box(
        modifier = modifier
//            .height(650.dp)
            .fillMaxWidth()
            .background(
                Color.Transparent,
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
                tauvm = tauvm,
                changeSheetType = changeSheetType
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.Inside(
    item: TauItem?,
    browser: IBrowser,
    sheetState: SheetState,
    tauvm: TauViewModel,
    changeSheetType: (SheetType) -> Unit
) {
    val bvm = browser.vm
    val state = bvm.state.collectAsState()
    val scope = rememberCoroutineScope()

    if (item == null)
        return

    Column(
        modifier = Modifier
    ) {
        if (state.value.isOpen) {
            browser.Render(
                modifier = Modifier.heightIn(max = 670.dp),
                exitImageSelection = {
                    browser.vm.close()
                    scope.launch {
                        sheetState.hide()
                        changeSheetType(SheetType.NONE)
                    }
                    browser.vm.changeState(target = null)
                }
            )
        } else {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                ApplicationFavorite(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 10.dp),
                    item = item, browser = browser, sheetState = sheetState,
                    tauvm = tauvm, changeSheetType = changeSheetType, bvm = bvm,
                    scope = scope,
                )

                BottomSheetHeader(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 10.dp),
                    item = item,
                )
            }

            Spacer(
                modifier = Modifier
                    .padding(start = 10.dp, end = 30.dp, top = 10.dp, bottom = 5.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.DarkGray)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                HtmlButton(
                    modifier = Modifier,
                    item = item, browser = browser, sheetState = sheetState,
                    tauvm = tauvm, changeSheetType = changeSheetType, bvm = bvm,
                    scope = scope
                )
            }
        }
    }
}

context(BoxScope)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationFavorite(
    modifier: Modifier,
    item: TauItem?,
    browser: IBrowser,
    sheetState: SheetState,
    tauvm: TauViewModel,
    changeSheetType: (SheetType) -> Unit,
    bvm: BrowserVM,
    scope: CoroutineScope,
) {
    if (item == null)
        return

    val appliFavos: AppliFavos by inject(AppliFavos::class.java)
    val favoris by appliFavos.appliFavorites.collectAsState()
    val isApplicationfavorite = favoris.contains(item)

    AsyncImage(
        modifier = Modifier
            .padding(end = 25.dp)
            .align(Alignment.TopEnd)
            .size(24.dp)
            .clickable{
                appliFavos.toggleApplicationFavorite(item)
            },
        model = if (isApplicationfavorite) R.drawable.star_fill else R.drawable.star,
        contentDescription = "Icone du titre",
        colorFilter = ColorFilter.tint(Color(0xFFE1D693))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.HtmlButton(
    modifier: Modifier = Modifier,
    item: TauItem?,
    browser: IBrowser,
    sheetState: SheetState,
    tauvm: TauViewModel,
    changeSheetType: (SheetType) -> Unit,
    bvm: BrowserVM,
    scope: CoroutineScope
) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.DarkGray,
            disabledContainerColor = Color.Gray
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TauColors.Tertiary),
        onClick = {
            bvm.changeState(
                isOpen = true,
                item = item,
                target = BrowserTarget.GOOGLE.withQuery(item?.name?.value ?: ""),
                onImageClicked = { imageUrl ->
                    browser.manageImageClick(
                        viewModel = tauvm,
                        imageUrl = imageUrl,
                        sheetState = sheetState,
                        scope = scope,
                        changeSheetType = changeSheetType
                    )
                }
            )
            scope.launch {
                sheetState.hide()
                sheetState.expand()
            }
        }
    ) {
        Text(
            modifier = Modifier
                .padding(0.dp),
            text = "Changer l'image"
        )
    }
}

context(BoxScope)
@Composable
fun BottomSheetHeader(
    modifier: Modifier,
    item: TauItem?
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
    ) {

        ////////////////////////
        // 1e ligne du header //
        ////////////////////////
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically)
                    .padding(start = 0.dp),
                model = R.drawable.title3,
                contentDescription = "Icone du titre",
            )

            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .align(Alignment.CenterVertically),
                text = item?.name?.value ?: "Aucun titre pour cet élément",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Companion.SemiBold,
                color = Color.DarkGray
            )
        }

        ////////////////////////
        // 2e ligne du header //
        ////////////////////////
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
        ) {
            ///////////////
            // extension //
            ///////////////
            val extension = if (item?.isFile() == true)
                item.name.value.substringAfterLast(".").uppercase()
            else null

            Row(
                modifier = Modifier,
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    if (extension != null) {
                        AsyncImage(
                            modifier = Modifier
                                .size(24.dp),
                            model = R.drawable.extensions,
                            contentDescription = "Icone du titre",
                        )

                        Text(
                            modifier = Modifier
                                .padding(start = 10.dp),
                            text = extension,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Companion.Normal,
                            color = Color.DarkGray
                        )

                        ////////////////
                        // séparateur //
                        ////////////////
                        Text(
                            modifier = Modifier
                                .padding(start = 10.dp, end = 10.dp),
                            text = "·",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Companion.Normal,
                            color = Color.DarkGray,
                        )
                    }
                }
            }

            //////////////////////////
            // date de modification //
            //////////////////////////
            val modificationDate = item?.modificationDate?.toddMMyyyyHHmmss()
            if (modificationDate != null) {

                AsyncImage(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically),
                    model = R.drawable.calendrier,
                    contentDescription = "Calendrier",
                )

                Text(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .align(Alignment.CenterVertically),
                    text = modificationDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Companion.Normal,
                    color = Color.DarkGray
                )
            }
        }
    }
}
