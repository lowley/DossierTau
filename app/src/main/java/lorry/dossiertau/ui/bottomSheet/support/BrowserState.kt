package lorry.dossiertau.ui.bottomSheet.support

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.isFile
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.name

@Immutable
data class BrowserState(
    val isOpen: Boolean = false,
    val item: TauItem? = null,
    val target: BrowserTarget? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val onImageClicked: (String) -> Unit = {},

    //valeur par défaut donnée
    //privé: pas inclus dans copy(...) donc garde la même valeur lors copys
    //mutable mais référence jamais changée donc classe @Immutable
    private val _bus: MutableSharedFlow<BrowserCommand> =
        MutableSharedFlow(replay = 0, extraBufferCapacity = 32)
) {
    val url: String? = computeUrl(item, target)
    val commands get() = _bus.asSharedFlow()
    fun send(cmd: BrowserCommand) {
        _bus.tryEmit(cmd)
    }

    fun goForward() = send(BrowserCommand.goForward)
    fun goBack() = send(BrowserCommand.goBack)

    fun computeUrl(
        item: TauItem?,
        target: BrowserTarget?
    ): String? {
        var searchString = ""
        val delimiter = ","

        if (target == null)
            return null

        if (item?.isFolder() == true) {
            val coreName = item.name.value
            val splitted = coreName.split(delimiter)
            if (splitted.size == 2)
                searchString = splitted.last()
            else if (splitted.size == 1)
                searchString = splitted.first()
        }

        if (item?.isFile() == true) {
            val coreName = item.name.value.substringBeforeLast(delimiter)

            val prepared1 = target
                .prepareSearchText(coreName)
                .split(' ')
                .filter {
                    it.isNotEmpty()
//                    && !it.matches(Regex("^\\(\\d{4}\\)$"))
                }

            val byPart = prepared1.indexOfFirst { it == "by" }

            val prepared2 = if (byPart != -1) {
                (byPart..prepared1.lastIndex).firstOrNull {
                    prepared1[it] == "-"
                }?.let { byIndex ->
                prepared1
                    .take(byIndex)
                    .filter { it != "by" }
            } ?: prepared1
        } else
            prepared1

        searchString = prepared2
            .joinToString("+") {
                it.replace("(", "")
                    .replace(")", "")
            }

    }

    return target.url + searchString
}
}

sealed interface BrowserCommand {
    object goBack : BrowserCommand
    object goForward : BrowserCommand
}

