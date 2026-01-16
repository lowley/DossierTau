package lorry.dossiertau.ui.bottomSheet.support

sealed class BrowserTarget(
    val url: String,
    val prepareSearchText: (itemName: String) -> String = { it },
    var suffix: String = ""
) {
    object GOOGLE : BrowserTarget(
        url = "https://www.google.com/search?q="
    )

    fun withQuery(query: String) = this.apply { suffix = query }
    fun computeUrl() = url + suffix
}