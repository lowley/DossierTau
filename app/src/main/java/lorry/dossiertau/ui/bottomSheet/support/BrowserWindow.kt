package lorry.dossiertau.ui.bottomSheet.support

import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.internal.ViewUtils.hideKeyboard

@Composable
fun BrowserWindow(
    modifier: Modifier = Modifier,
    browserState: BrowserState,
    onImageClicked: (String) -> Unit,
    setCanGoBack: (Boolean) -> Unit,
    setCanGoForward: (Boolean) -> Unit,
    closeBrowser: () -> Unit,
) {
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
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    if (!v.hasFocus()) {
                        v.requestFocus()
                        requestFocusFromTouch()
                    }

//                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
//                            as InputMethodManager
//                    post { imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT) }
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
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(webView.windowToken, 0)
            webView.destroy()
        }
    }

    // ⚠️ Charger l’URL UNIQUEMENT quand le paramètre change
    val lastLoadedFromState = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(browserState.url) {
        val target = browserState.url
        if (target != null && target != lastLoadedFromState.value) {
            webView.loadUrl(target)
            lastLoadedFromState.value = target
        }
    }

// Le `update` ne doit plus appeler loadUrl
    AndroidView(
        modifier = modifier,
        factory = { webView },
        update = { /* rien de navigation ici ; à la rigueur focus/IME */ }
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


//Toast.makeText(
//context,
//"Naviguez et appuyez longuement sur l'image choisie",
//Toast.LENGTH_LONG
//)
//.show()