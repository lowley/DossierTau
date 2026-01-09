package lorry.dossiertau.ui.support.capsule.utilities

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

class FileCapsuleIO: ICapsuleIO {

    private val CHARSET = "UTF-8"

    private val START_CAPSULE = "##SIGMA-COMPOSITE-START##"

    //ici il y aura le composite
    private val END_CAPSULE = "##SIGMA-COMPOSITE-END##"
    private val START_LENGTH = "##SIGMA-LENGTH-START##"
    private val END_LENGTH = "##SIGMA-LENGTH-END##"

    
    override suspend fun getCapsule(filePath: String): CapsuleData? {

        val file = File(filePath)
        val charset = Charset.forName(CHARSET)
        var extractedText: String? = null

        try {
            RandomAccessFile(file, "r").use { raf ->
                val length = raf.length()
                val (tail, _, _) = tryToExtract(
                    length, raf, charset, filePath = filePath
                )

                extractedText = tail
                if (tail == null) {
                    return@use
                }
            }

            //composite pas trouvé malgré tous les essais

        } catch (ex: Exception) {
            println("erreur getComposite: ${ex.message}")
            extractedText = null
        }

        if (extractedText == null)
            return null

        val extractedCapsule =
            Gson().fromJson<CapsuleData>(extractedText, CapsuleData::class.java)

        return extractedCapsule
    }

    override suspend fun replaceCapsule(filePath: String, capsule: CapsuleData?
    ): Boolean {
        val file = File(filePath)
        if (!file.exists()) {
            if (filePath.endsWith(".html")) {
                createFolderHtmlFile(filePath)
            } else return false
        }

//        if (getComposite(filePath) != null)
//            removeCompositeAndDatas(filePath)

        val data = Gson().toJson(capsule)
        saveData(filePath, data = data, START_CAPSULE, END_CAPSULE)

        val dataLength = data.length.toString()
        saveData(filePath, data = dataLength, START_LENGTH, END_LENGTH)

        val verification = getCapsule(filePath)
        return verification == capsule
    }

    /**
     * ajoute data à la fin du fichier, entouré des balises "start" et "end"
     */
    private fun saveData(filePath: String, data: String, start: String, end: String) {
        val file = File(filePath)
        if (!file.exists())
            return

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(raf.length())
            val insert = "$start\n$data\n$end\n"
            raf.write(insert.toByteArray(Charset.forName(CHARSET)))
        }
    }

    suspend fun removeCapsuleAndDatas(
        filePath: String,
    ) {
        ///////////////////////
        // lecture de length //
        ///////////////////////
        val file = File(filePath)
        val length = file.length()
        val charset = Charset.forName(CHARSET)

        RandomAccessFile(file, "rw").use { raf ->
            val firstSeekLength = minOf(10000.toLong(), length)
            val firstSeekStart = length - firstSeekLength
            //position de départ
            raf.seek(firstSeekStart)
            //ce qui est lu cette fois-ci, au max longueur initialLookbackBytes
            val firstSeekBytes = ByteArray(firstSeekLength.toInt())
            //lecture jusqu'à gaver bytes
            raf.readFully(firstSeekBytes)

            //lecture des bytes et recherche
            val firstSeekTail = String(firstSeekBytes, charset)
            val firstSeekStartIndex = firstSeekTail.lastIndexOf(START_LENGTH)
            val firstSeekEndIndex = firstSeekTail.lastIndexOf(END_LENGTH)

            val firstSeekResult =
                if (firstSeekStartIndex != -1 && firstSeekEndIndex != -1 && firstSeekEndIndex > firstSeekStartIndex)
                    firstSeekTail.substring(firstSeekStartIndex + START_LENGTH.length, firstSeekEndIndex)
                        .trim().toLong()
                else 0L
//            println("recherche length pour fichier ${filePath.substringAfterLast("/")}: $firstSeekResult")

            //////////////////////////
            // lecture du composite //
            //////////////////////////
            val firstSeekSTART_LENGTHtoEnd = firstSeekLength - firstSeekStartIndex
            val secondSeekEnd = length - firstSeekSTART_LENGTHtoEnd - 1
            //longueurs des contenus des variables START_CAPSULE + END_CAPSULE = 48
            val addition = 2
            val secondSeekStart = secondSeekEnd - firstSeekResult - START_CAPSULE.length - END_CAPSULE
                .length - addition
            
            //position de départ
//            raf.seek(0)
            //ce qui est lu cette fois-ci, au max longueur initialLookbackBytes
//            val secondSeekBytes = ByteArray(length.toInt())
            //lecture jusqu'à gaver bytes
//            raf.readFully(secondSeekBytes)

            //lecture des bytes et recherche
//            val secondSeekTail = String(secondSeekBytes, charset)
//            val secondSeekStartIndex = secondSeekTail.lastIndexOf(START_CAPSULE)
//            val secondSeekEndIndex = secondSeekTail.lastIndexOf(END_CAPSULE)
//
//            val cutIndex = length - secondSeekBytes.size + secondSeekStartIndex

//            if (secondSeekStartIndex != -1 && secondSeekEndIndex != -1 && secondSeekEndIndex > secondSeekStartIndex) {
            raf.setLength(secondSeekStart.toLong())
//            }
        }
    }

    private fun tryToExtract(
        length: Long,
        raf: RandomAccessFile,
        charset: Charset,
        filePath: String,
    ): Triple<String?, Int, Int> {

        ///////////////////////
        // lecture de length //
        ///////////////////////
        val firstSeekLength = minOf(10000.toLong(), length)
        val firstSeekStart = length - firstSeekLength
        //position de départ
        raf.seek(firstSeekStart)
        //ce qui est lu cette fois-ci, au max longueur initialLookbackBytes
        val firstSeekBytes = ByteArray(firstSeekLength.toInt())
        //lecture jusqu'à gaver bytes
        raf.readFully(firstSeekBytes)

        //lecture des bytes et recherche
        val firstSeekTail = String(firstSeekBytes, charset)
        val firstSeekStartIndex = firstSeekTail.lastIndexOf(START_LENGTH)
        val firstSeekEndIndex = firstSeekTail.lastIndexOf(END_LENGTH)

        val firstSeekResult =
            if (firstSeekStartIndex != -1 && firstSeekEndIndex != -1 && firstSeekEndIndex > firstSeekStartIndex)
                firstSeekTail.substring(firstSeekStartIndex + START_LENGTH.length, firstSeekEndIndex).trim()
                    .toLong()
            else 0L
//        println("FICHIER ${filePath.substringAfterLast("/")}\t(${File(filePath).length()})\tcomposite $firstSeekResult")

        ////////////////////////
        // lecture du capsule //
        ////////////////////////
        val firstSeekSTART_LENGTHtoEnd = firstSeekLength - firstSeekStartIndex
        val secondSeekEnd = length - firstSeekSTART_LENGTHtoEnd
        //longueurs des contenus des variables START_CAPSULE + END_CAPSULE = 48
        val secondSeekStart = maxOf(secondSeekEnd - firstSeekResult - 100, 0)
        //position de départ
        raf.seek(secondSeekStart)
        //ce qui est lu cette fois-ci, au max longueur initialLookbackBytes
        val secondSeekBytes = ByteArray(firstSeekResult.toInt() + 100)
        //lecture jusqu'à gaver bytes
        raf.readFully(secondSeekBytes)

        //lecture des bytes et recherche
        val secondSeekTail = String(secondSeekBytes, charset)
        val secondSeekStartIndex = secondSeekTail.lastIndexOf(START_CAPSULE)
        val secondSeekEndIndex = secondSeekTail.lastIndexOf(END_CAPSULE)

        val result =
            if (secondSeekStartIndex != -1 && secondSeekEndIndex != -1 && secondSeekEndIndex > secondSeekStartIndex)
                secondSeekTail.substring(secondSeekStartIndex + START_CAPSULE.length, secondSeekEndIndex)
                    .trim()
            else null

        return Triple(result, secondSeekStartIndex, secondSeekEndIndex)
    }
}

suspend fun createFolderHtmlFile(filePath: String) {

    //picture contient un bitmap
    createShortcut(baseText(), filePath)
}

suspend fun createShortcut(text: String, fullPathAndName: String) {
    withContext(Dispatchers.IO) {
        val fichier = File(fullPathAndName)
        fichier.writeText(text, Charsets.UTF_8)
    }
}

fun baseText(): String {
    val text = """<!DOCTYPE html>
                                 <html lang="fr">
                                 <head>
                                     <meta charset="UTF-8">
                                     <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                     <title>Image container</title>
                                 </head>
                                 <body>
                                   
                                 </body>
                                 </html>"""

    return text
}

