package lorry.dossiertau.ui.bottomSheet.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.model.TauItem
import java.net.HttpURLConnection
import java.net.URL

class BrowserViewModel: ViewModel() {

    ///////////////////
    // browser state //
    ///////////////////
    private val _state: MutableStateFlow<BrowserState> = MutableStateFlow<BrowserState>(
        BrowserState())
    val state: StateFlow<BrowserState> = _state

    fun open(item: TauItem, target: BrowserTarget) {
        _state.update { it.copy(isOpen = true, item = item, target = target) }
    }

    fun close() {
        _state.update { it.copy(isOpen = false, item = null, target = null) }
    }

    fun changeState(
        isOpen: Boolean = _state.value.isOpen,
        item: TauItem? = _state.value.item,
        target: BrowserTarget? = _state.value.target,
        canGoBack: Boolean = _state.value.canGoBack,
        canGoForward: Boolean = _state.value.canGoForward,
        onImageClicked: (String) -> Unit = _state.value.onImageClicked,
    ){
        _state.update { it.copy(
            isOpen = isOpen,
            item = item,
            target = target,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onImageClicked = onImageClicked
        ) }


    }

    suspend fun urlToBitmap(data: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            return@withContext if (data.startsWith("data:image")) {
                // 🟢 C'est une image encodée en base64
                val base64Data = data.replace("data:image/png;base64,", "").replace("data:image/jpeg;base64,","")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } else {
                // 🌐 C'est une URL réseau
                val connection = URL(data).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream).also {
                    inputStream.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}