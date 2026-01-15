package lorry.dossiertau.ui.bottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.name

@Composable
fun ItemContent(item: TauItem?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .width(40.dp)
            .background(
                Color.LightGray,
                RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp
                )
            )
    )
    {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("item: ${item?.name}")
            Button(onClick = { /* Action 1 */ }) { Text("Option 1") }
            Button(onClick = { /* Action 2 */ }) { Text("Option 2") }
        }
    }
}