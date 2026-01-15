package lorry.dossiertau.ui.bottomSheet.support

sealed class BrowserTarget(
    val url: String,
    val prepareSearchText: (itemName: String) -> String = { it }
) {
    object GOOGLE : BrowserTarget(
        url = "https://www.google.com/search?q="
    )
}