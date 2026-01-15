package lorry.dossiertau.ui.bottomSheet.support

import kotlinx.coroutines.flow.StateFlow
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.isFile
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.name

fun elaborateSearchString(
    itemFlow: StateFlow<TauItem?>,
    targetFlow: StateFlow<BrowserTarget?>): String {
    var final = ""

    val item = itemFlow.value
    val target = targetFlow.value

    if (item == null || target == null)
        return ""


    if (item.isFolder()) {
        val coreName = item.name.value
        val splitted = coreName.split(".")
        if (splitted.size == 2)
            final = splitted.last()
    }

    if (item.isFile()) {
        val coreName = item.name.value.substringBeforeLast(".")

        val prepared1 = target
            .prepareSearchText(coreName)
            .split(' ')
            .filter {
                it.isNotEmpty()
//                    && !it.matches(Regex("^\\(\\d{4}\\)$"))
            }

        val byPart = prepared1.indexOfFirst { it == "by" }

        val prepared2 = if (byPart != -1) {
            val tiretIndex = prepared1.indexOfFirst {
                it == "-" && prepared1.indexOf(it) > byPart
            }
            prepared1
                .take(tiretIndex)
                .filter { it != "by" }
        } else prepared1

        final = prepared2
            .joinToString("+") {
                it.replace("(", "")
                    .replace(")", "")
            }

    }

    return final
}
