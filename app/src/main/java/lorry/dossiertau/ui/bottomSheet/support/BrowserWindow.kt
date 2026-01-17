package lorry.dossiertau.ui.bottomSheet.support

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BrowserWindow(
    modifier: Modifier = Modifier
        .pointerInput(Unit) {
            awaitPointerEventScope {
                val down = awaitFirstDown()
                gestureOwner.value = GestureOwner.Sheet
                waitForUpOrCancellation()
                gestureOwner.value = GestureOwner.None
            }
        },
    browserState: BrowserState,
    onImageClicked: (String) -> Unit,
    setCanGoBack: (Boolean) -> Unit,
    setCanGoForward: (Boolean) -> Unit,
    closeBrowser: () -> Unit,
    gestureOwner: MutableState<GestureOwner>,
) {
    Column(modifier = modifier.fillMaxSize()) {

        val context = LocalContext.current

        val webView = remember {
            WebView(context).apply {
                // Focus clavier
                isFocusable = true
                isFocusableInTouchMode = true

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        setCanGoBack(view.canGoBack())
                        setCanGoForward(view.canGoForward())

                        // Détection long-press image -> callback Kotlin
                        evaluateJavascript(
                            """
                        (function(){
                          document.addEventListener('contextmenu', function(e){
                            var el = e.target; 
                            if (el && el.tagName === 'IMG') {
                              e.preventDefault();
                              window.android.onImageLongClick(el.src);
                            }
                          }, {passive:false});
                        })();
                        """.trimIndent(),
                            null
                        )
                    }
                }

                // 1er touch : focus réel + ouverture IME
                setOnTouchListener { v, ev ->
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            gestureOwner.value = GestureOwner.WebView
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            gestureOwner.value = GestureOwner.None
                        }
                    }

                    if (ev.action == MotionEvent.ACTION_DOWN) {
                        if (!v.hasFocus()) {
                            v.requestFocus()
                            requestFocusFromTouch()
                        }
                    }
                    false // ne pas consommer, laisser WebView gérer
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onImageLongClick(imageUrl: String) {
                            onImageClicked(imageUrl)
                            closeBrowser()
                        }
                    },
                    "android"
                )
            }
        }

        // Fermer le clavier proprement quand on enlève le WebView
        DisposableEffect(Unit) {
            onDispose {
                val imm =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(webView.windowToken, 0)
                webView.destroy()
            }
        }

        // ⚠️ Charger l’URL UNIQUEMENT quand le paramètre change
        val lastLoadedFromState = remember { mutableStateOf<String?>(null) }
        LaunchedEffect(browserState.url) {
            val target = browserState.computeUrl(browserState.item, browserState.target)
            if (target != null && target != lastLoadedFromState.value) {
                webView.loadUrl(target)
                lastLoadedFromState.value = target
            }
        }

// Le `update` ne doit plus appeler loadUrl

        AndroidView<WebView>(
            modifier = modifier
                .weight(1f),
//                .navigationBarsPadding(),
            factory = { ctx ->
                webView.apply {
                    // Indispensable : active la communication de scroll avec la BottomSheet
                    isNestedScrollingEnabled = true

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                // On demande à la BottomSheet de ne pas intercepter
                                v.parent.requestDisallowInterceptTouchEvent(true)
                            }

                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false // Permet à la WebView de traiter le scroll normalement
                    }
                }
            }
        )

        BrowserBottomToolbar(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )

        LaunchedEffect(Unit) {
            browserState.commands.collect { cmd ->
                when (cmd) {
                    is BrowserCommand.goBack -> if (webView.canGoBack()) webView.goBack()
                    is BrowserCommand.goForward -> if (webView.canGoForward()) webView.goForward()
                }
            }
        }
    }
}


//Toast.makeText(
//context,
//"Naviguez et appuyez longuement sur l'image choisie",
//Toast.LENGTH_LONG
//)
//.show()

enum class GestureOwner {
    WebView,
    Sheet,
    None
}

