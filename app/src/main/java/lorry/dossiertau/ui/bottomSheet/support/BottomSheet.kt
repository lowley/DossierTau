package lorry.dossiertau.ui.bottomSheet.support

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lorry.dossiertau.MainActivity
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.name

@Composable
fun MainActivity.BottomSheetContent(
    type: BottomSheetType,
    item: TauItem?) {
    when (type) {
        BottomSheetType.APPLICATION -> {
            var text by remember { mutableStateOf("truc") }
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Saisir du texte") }
                )
                Button(onClick = { /* Action sur text */ }) {
                    Text("Valider")
                }
            }
        }

        BottomSheetType.ITEM -> {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("item: ${item?.name}")
                Button(onClick = { /* Action 1 */ }) { Text("Option 1") }
                Button(onClick = { /* Action 2 */ }) { Text("Option 2") }
            }
        }
    }
}