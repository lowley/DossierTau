package lorry.dossiertau.ui.support.capsule.utilities

import com.google.gson.Gson
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import kotlin.math.min

/**
 * le nouveau, utilisé si pas useOld
 */
class FileMetadataManager() : ICapsuleIO {

    private val gson = Gson()
    
    private val CHARSET_NAME = "UTF-8"
    private val CHARSET = Charset.forName(CHARSET_NAME)

    private val START_CAPSULE_TAG_BYTES = "##SIGMA-METADATA-START##\n".toByteArray(CHARSET)
    private val END_CAPSULE_TAG_BYTES = "\n##SIGMA-METADATA-END##\n".toByteArray(CHARSET)
    // Notez les \n inclus pour un calcul plus précis de la taille totale.

    private val START_METADATA_LENGTH_TAG = "##SIGMA-METADATA-LENGTH-START##"
    private val END_METADATA_LENGTH_TAG = "##SIGMA-METADATA-LENGTH-END##"
    // Pas besoin de version _BYTES pour ceux-ci car on les cherche dans une String lue pour obtenir la valeur de longueur.

    // Buffer pour lire la section contenant la longueur totale des métadonnées.
    private val METADATA_LENGTH_LOOKBACK_BUFFER_SIZE =
        256 // Suffisant pour les balises de longueur et la valeur.

    /**
     * Contient les informations lues sur les métadonnées.
     */
    private data class ParsedMetadata(
        val capsuleData: CapsuleData?,
        val totalMetadataBlockByteLength: Long, // Longueur en octets de tout le bloc (composite + ses balises)
        val metadataBlockStartOffsetInFile: Long // Offset où commence le START_COMPOSITE_TAG
    )

    private suspend fun readExistingMetadataInfo(raf: RandomAccessFile, fileLength: Long, fileName: String): ParsedMetadata? {
//        if (fileLength < METADATA_LENGTH_LOOKBACK_BUFFER_SIZE) {
////            println("SIGMALOG Fichier $fileName readExistingMetadataInfo:  trop petit pour les balises de " +
////                    "longueur de métadonnées.")
//            return null
//        }

        // 1. Lire la section de la longueur totale des métadonnées
        val lengthBuffer =
            ByteArray(min(fileLength, METADATA_LENGTH_LOOKBACK_BUFFER_SIZE.toLong()).toInt())
        val lengthSectionReadStartOffset = fileLength - lengthBuffer.size
        raf.seek(lengthSectionReadStartOffset)
        raf.readFully(lengthBuffer)
        val lengthSectionContent = String(lengthBuffer, CHARSET)

        val totalMetadataByteLengthString =
            extractLastContent(lengthSectionContent, START_METADATA_LENGTH_TAG, END_METADATA_LENGTH_TAG)
        if (totalMetadataByteLengthString == null) {
//            println("SIGMALOG fichier $fileName readExistingMetadataInfo:  Balises de longueur totale des " +
//                    "métadonnées " +
//                    "non trouvées.")
            return null
        }
        val totalMetadataByteLength = totalMetadataByteLengthString.trim().toLongOrNull()
        if (totalMetadataByteLength == null || totalMetadataByteLength <= 0) {
//            println("SIGMALOG fichier $fileName readExistingMetadataInfo: , Longueur totale des métadonnées" +
//                    " " +
//                    "invalide: " +
//                    "'$totalMetadataByteLengthString'")
            return null
        }

        // 2. Reconstruire la chaîne exacte du "bloc de longueur" tel qu'il aurait été écrit
        val actualLengthBlockStringContent = totalMetadataByteLengthString.trim() // La valeur numérique
        val actualLengthBlockFullString =
            "$START_METADATA_LENGTH_TAG\n$actualLengthBlockStringContent\n$END_METADATA_LENGTH_TAG\n"
        val actualLengthBlockBytesSize = actualLengthBlockFullString.toByteArray(CHARSET).size.toLong()
        
        val actualCapsuleBlockStartOffset =
            fileLength - actualLengthBlockBytesSize - totalMetadataByteLength

        if (actualCapsuleBlockStartOffset < 0 || totalMetadataByteLength > fileLength) {
//            println("SIGMALOG fichier $fileName readExistingMetadataInfo: , Calcul d'offset de métadonnées " +
//                    "invalide.")
            return null
        }

        val capsuleBlockBuffer = ByteArray(totalMetadataByteLength.toInt())
        raf.seek(actualCapsuleBlockStartOffset)
        raf.readFully(capsuleBlockBuffer)

        // Extraire le JSON du composite du compositeBlockBuffer
        // On doit retirer les START_COMPOSITE_TAG_BYTES et END_COMPOSITE_TAG_BYTES
        val jsonStartOffset = START_CAPSULE_TAG_BYTES.size
        val jsonEndOffset = capsuleBlockBuffer.size - END_CAPSULE_TAG_BYTES.size

        if (jsonStartOffset >= jsonEndOffset || !capsuleBlockBuffer.copyOfRange(
                0,
                START_CAPSULE_TAG_BYTES.size
            ).contentEquals(START_CAPSULE_TAG_BYTES) ||
            !capsuleBlockBuffer.copyOfRange(jsonEndOffset, capsuleBlockBuffer.size)
                .contentEquals(END_CAPSULE_TAG_BYTES)
        ) {
//            println("SIGMALOG fichier $fileName readExistingMetadataInfo: , Balises de composite non " +
//                    "trouvées ou " +
//                    "corrompues dans le bloc lu.")
            // Cela pourrait indiquer un problème avec totalMetadataByteLength stocké
            return null
        }

        val capsuleJsonBytes = capsuleBlockBuffer.copyOfRange(jsonStartOffset, jsonEndOffset)
        val capsuleJsonString = String(capsuleJsonBytes, CHARSET).trim() // trim() au cas où
//        println("capsuleJsonString: $capsuleJsonString")
        val capsuleData = try {
            gson.fromJson(capsuleJsonString, CapsuleData::class.java)
        } catch (e: Exception) {
//            println("SIGMALOG fichier $fileName readExistingMetadataInfo:  Erreur de désérialisation du " +
//                    "JSON $capsuleJsonString: ${e
//                .message}")
            null
        }

        return ParsedMetadata(
            capsuleData = capsuleData,
            totalMetadataBlockByteLength = totalMetadataByteLength, // C'est la longueur du bloc composite (avec ses balises)
            metadataBlockStartOffsetInFile = actualCapsuleBlockStartOffset
        )
    }


    override suspend fun getCapsule(filePath: String): CapsuleData? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return null

        try {
            RandomAccessFile(file, "r").use { raf ->
                val fileLength = raf.length()
                if (fileLength == 0L) return@use null
                val composite = readExistingMetadataInfo(
                    raf, fileLength, filePath
                )?.capsuleData

//                println(
//                    "SIGMALOG fichier ${
//                        filePath.substringAfterLast("/").take(20).padEnd(20).padEnd(20)
//                    } getComposite: " +
//                            "lecture " +
//                            "composite: $composite"
//                )
                return composite
            }
        } catch (e: Exception) {
//            println(
//                "SIGMALOG fichier ${
//                    filePath.substringAfterLast("/").take(20).padEnd(20)
//                } getComposite: erreur " +
//                        "lecture " +
//                        "composite ($filePath): ${e.message}"
//            )
            return null
        }
        
        return null
    }

    override suspend fun replaceCapsule(filePath: String, newCapsule: CapsuleData?): Boolean {
        val file = File(filePath)
        if (!file.exists() && !filePath.endsWith(".html", ignoreCase = true)) {
//            println("SIGMA fichier ${filePath.substringAfterLast("/").take(20).padEnd(20)} replaceComposite: Fichier " +
//                    "non trouvé (et n'est pas un HTML à créer) pour " +
//                    "remplacement")
            return false
        }
        if (filePath.endsWith(".html", ignoreCase = true) && !file.exists()) {
            // createFolderHtmlFile(filePath) // Si vous avez cette logique
//            println("SIGMALOG: fichier ${filePath.substringAfterLast("/")} replaceComposite Fichier HTML " +
//                    "créé. Ajout initial de " +
//                    "composite" +
//                    "...")
            // Pour un nouveau fichier, pas de troncature, juste un ajout.
        }

        //////////////////////////////////////////
        // voir pq coupure semble pas effective //
        // algo correct?                        //
        //////////////////////////////////////////

        try {

            //we save the date; cf [[restoration]]
            var realPath = filePath
            if (filePath.endsWith("/.folderPicture.html"))
                realPath = realPath.substringBeforeLast("/")
            val parent: Path = Paths.get(realPath)
            val child: Path = Paths.get(filePath)
            val oldTime = parent.toFile().lastModified()

            var raf = RandomAccessFile(file, "rw")

            val fileLength = raf.length()
            var positionToTruncate = fileLength

            val existingMetadata = if (fileLength > 0) readExistingMetadataInfo(raf, fileLength, 
                fileName = filePath) 
            else null

            if (existingMetadata != null) {
                // La position à tronquer est le début du bloc composite existant
                val lengthOfLengthBlock =
                    fileLength - existingMetadata.metadataBlockStartOffsetInFile - existingMetadata.totalMetadataBlockByteLength
                positionToTruncate =
                    fileLength - existingMetadata.totalMetadataBlockByteLength - lengthOfLengthBlock
                // ou plus simplement, si totalMetadataByteLength stocké INCLUT le bloc de longueur aussi:
                // positionToTruncate = fileLength - existingMetadata.totalMetadataBlockByteLength
                // OU, si ParsedMetadata est bien défini :
                // positionToTruncate = existingMetadata.metadataBlockStartOffsetInFile
            }


            // Ajustement crucial: la position de troncature doit être le début du *premier* bloc de méta (le composite)
            if (existingMetadata != null) {
                positionToTruncate = existingMetadata.metadataBlockStartOffsetInFile
            }


            if (positionToTruncate < fileLength) {
//                println("SIGMALOG fichier ${filePath.substringAfterLast("/").take(20).padEnd(20)} replaceComposite: Troncature à $positionToTruncate " +
//                        "(longueur" +
//                        " originale $fileLength)")
                raf.setLength(positionToTruncate)
            }
            raf.close()

//            return true


            raf = RandomAccessFile(file, "rw")
            raf.seek(positionToTruncate) // Se positionner à la (nouvelle) fin

            if (newCapsule != null) {
                val capsuleJsonString = gson.toJson(newCapsule)
                val capsuleJsonBytes = capsuleJsonString.toByteArray(CHARSET)
                
                val capsuleBlockToWrite =
                    START_CAPSULE_TAG_BYTES + capsuleJsonBytes + END_CAPSULE_TAG_BYTES
                val totalCapsuleBlockByteLength = capsuleBlockToWrite.size.toLong()

                // Écrire le bloc composite
                raf.write(capsuleBlockToWrite)

//                println("SIGMALOG fichier ${filePath.substringAfterLast("/").take(20).padEnd(20)} replaceComposite: " +
//                        "Ecrit composite #1: $capsuleJsonString")
                
                // Préparer et écrire le bloc de longueur
                val lengthValueString = totalCapsuleBlockByteLength.toString()
                val lengthBlockString =
                    "$START_METADATA_LENGTH_TAG\n$lengthValueString\n$END_METADATA_LENGTH_TAG\n"
                val lengthBlockBytes = lengthBlockString.toByteArray(CHARSET)
                raf.write(lengthBlockBytes)
//                println("SIGMALOG fichier ${filePath.substringAfterLast("/").take(20).padEnd(20)} replaceComposite: " +
//                        "Ecrit composite #2: $lengthBlockString")
            }

            raf.close()

            //#[[restoration]]
            val seenChild = FileMTime.restoreLastModified(child.toFile(), oldTime)
            val seenParent = FileMTime.restoreLastModified(parent.toFile(), oldTime)
//            val old = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(oldTime),
//                ZoneId.systemDefault()).format(ISO_OFFSET_TIME)
//            val seenC2 = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(seenChild),
//                ZoneId.systemDefault()).format(ISO_OFFSET_TIME)
//            val seenP2 = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(seenParent),
//                ZoneId.systemDefault()).format(ISO_OFFSET_TIME)
//            println("old: $old, parent: $seenP2, .folderPicture: $seenC2")

            // Vérification
            if (newCapsule != null) {
                val verification = getCapsule(filePath)
                val result = verification == newCapsule
                
//                println("SIGMALOG fichier ${filePath.substringAfterLast("/").take(20).padEnd(20)} replaceComposite: " +
//                        "Vérification: $result")
                return result
            }
            return true // Succès (même si newComposite était null, la troncature a fonctionné)

        } catch (e: Exception) {
//            println("SIGMALOG fichier ${filePath.substringAfterLast("/").take(20).padEnd(20)} replaceComposite: Erreur" +
//                    " ($filePath): ${e.message}")
            return false
        }
    }

    private fun extractLastContent(source: String, startTag: String, endTag: String): String? {
        val endIndex = source.lastIndexOf(endTag)
        if (endIndex == -1) return null
        val startIndex = source.lastIndexOf(startTag, endIndex)
        if (startIndex == -1 || startIndex >= endIndex) return null
        return source.substring(startIndex + startTag.length, endIndex).trim()
    }

    fun snapshotLastModified(path: Path): FileTime =
        Files.getLastModifiedTime(path)

    fun restoreLastModified(path: Path, originalTime: FileTime) {
        Files.setLastModifiedTime(path, originalTime)
    }
}
