package lorry.dossiertau.ui.bottomSheet.contents

import android.content.Intent
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
import lorry.dossiertau.TpdbTestActivity
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.isFile
import lorry.dossiertau.data.model.modificationDate
import lorry.dossiertau.data.model.name
import lorry.dossiertau.data.model.toFavorite
import lorry.dossiertau.ui.bottomSheet.support.SheetType
import lorry.dossiertau.ui.bottomSheet.browser.support.BrowserTarget
import lorry.dossiertau.ui.bottomSheet.browser.BrowserVM
import lorry.dossiertau.ui.bottomSheet.browser.IBrowser
import lorry.dossiertau.usecases.applicationFavorites.AppliFavos
import lorry.dossiertau.usecases.applicationFavorites.contains
import lorry.folder.items.dossiersigma.external.userPreferences.PrefsAppliFavo
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.SheetContentLevelItem(item: TauItem?, sheetState: SheetState, tauvm: TauViewModel, modifier: Modifier) {
    Box(modifier = modifier.fillMaxWidth().background(Color.Transparent, RoundedCornerShape(topStart=8.dp,topEnd=8.dp))) { Column { Inside(item=item,sheetState=sheetState,tauvm=tauvm) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.Inside(item: TauItem?, sheetState: SheetState, tauvm: TauViewModel) {
    val browser: IBrowser by inject<IBrowser>(); val bvm=browser.vm; val state=bvm.state.collectAsState(); val scope=rememberCoroutineScope(); if(item==null)return
    Column { if(state.value.isOpen){browser.Render(modifier=Modifier.heightIn(max=670.dp),exitImageSelection={browser.vm.close();scope.launch{sheetState.hide();bsVM.changeType(SheetType.NONE)};browser.vm.changeState(target=null)},bsVM=bsVM)} else { Box(Modifier.fillMaxWidth()){ApplicationFavorite(Modifier.align(Alignment.TopEnd).padding(end=10.dp),item,browser,sheetState,tauvm,bvm,scope);BottomSheetHeader(Modifier.align(Alignment.CenterStart).padding(horizontal=10.dp),item)};Spacer(Modifier.padding(start=10.dp,end=30.dp,top=10.dp,bottom=5.dp).fillMaxWidth().height(1.dp).background(Color.DarkGray));Row(Modifier.fillMaxWidth().padding(horizontal=10.dp)){HtmlButton(Modifier,item,browser,sheetState,tauvm,bvm,scope);if(item.isFile()){Spacer(Modifier.size(8.dp));TpdbButton(item)}} } }
}

@Composable
fun MainActivity.TpdbButton(item: TauItem){Button(colors=ButtonDefaults.outlinedButtonColors(containerColor=Color.Transparent,contentColor=Color.DarkGray,disabledContainerColor=Color.Gray),shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,TauColors.Tertiary),onClick={startActivity(Intent(this,TpdbTestActivity::class.java).putExtra(TpdbTestActivity.EXTRA_FILENAME,item.name.value))}){Text(text="Tester TPDB")}}

context(BoxScope)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationFavorite(modifier:Modifier,item:TauItem?,browser:IBrowser,sheetState:SheetState,tauvm:TauViewModel,bvm:BrowserVM,scope:CoroutineScope){if(item==null)return;val appliFavos:AppliFavos by inject(AppliFavos::class.java);val favoris by appliFavos.appliFavorites.collectAsState(emptyList());val isApplicationfavorite=favoris.contains(item);val prefsAppliFavo:PrefsAppliFavo by inject(PrefsAppliFavo::class.java);AsyncImage(modifier=modifier.padding(end=25.dp).align(Alignment.TopEnd).size(24.dp).clickable{appliFavos.toggleApplicationFavorite(item);scope.launch{if(isApplicationfavorite)prefsAppliFavo.removeAppliFavo(item.toFavorite())else prefsAppliFavo.addAppliFavo(item.toFavorite())}},model=if(isApplicationfavorite)R.drawable.star_fill else R.drawable.star,contentDescription="Icone du titre",colorFilter=ColorFilter.tint(Color(0xFFE1D693)))}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.HtmlButton(modifier:Modifier=Modifier,item:TauItem?,browser:IBrowser,sheetState:SheetState,tauvm:TauViewModel,bvm:BrowserVM,scope:CoroutineScope){Button(modifier=modifier,colors=ButtonDefaults.outlinedButtonColors(containerColor=Color.Transparent,contentColor=Color.DarkGray,disabledContainerColor=Color.Gray),shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,TauColors.Tertiary),onClick={bvm.changeState(isOpen=true,item=item,target=BrowserTarget.GOOGLE.withQuery(item?.name?.value?:""),onImageClicked={imageUrl->browser.manageImageClick(viewModel=tauvm,imageUrl=imageUrl,sheetState=sheetState,scope=scope,bsVm=bsVM)});scope.launch{sheetState.hide();sheetState.expand()}}){Text(text="Changer l'image",modifier=Modifier.padding(0.dp))}}

context(BoxScope)
@Composable
fun BottomSheetHeader(modifier:Modifier,item:TauItem?){Column(modifier.wrapContentHeight()){Row(Modifier.fillMaxWidth()){AsyncImage(Modifier.size(24.dp).align(Alignment.CenterVertically),R.drawable.title3,"Icone du titre");Text(text=item?.name?.value?:"Aucun titre pour cet élément",modifier=Modifier.padding(start=10.dp).align(Alignment.CenterVertically),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,color=Color.DarkGray)};Row(Modifier.fillMaxWidth().padding(top=5.dp)){val extension=if(item?.isFile()==true)item.name.value.substringAfterLast(".").uppercase() else null;if(extension!=null){AsyncImage(Modifier.size(24.dp),R.drawable.extensions,"Icone du titre");Text(text=extension,modifier=Modifier.padding(start=10.dp),style=MaterialTheme.typography.titleMedium,color=Color.DarkGray);Text(text="·",modifier=Modifier.padding(start=10.dp,end=10.dp),style=MaterialTheme.typography.titleMedium,color=Color.DarkGray)};val modificationDate=item?.modificationDate?.toddMMyyyyHHmmss();if(modificationDate!=null){AsyncImage(Modifier.size(20.dp).align(Alignment.CenterVertically),R.drawable.calendrier,"Calendrier");Text(text=modificationDate,modifier=Modifier.padding(start=10.dp).align(Alignment.CenterVertically),style=MaterialTheme.typography.titleMedium,color=Color.DarkGray)}}}}
