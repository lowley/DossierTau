package lorry.dossiertau.data.base64

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.layout.ContentScale
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.ColoredTag
import lorry.folder.items.dossiersigma.external.base64.IVideoInfoEmbedder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

//ordre:
//Base64
//Scale
//Tag


class VideoInfoEmbedder: IVideoInfoEmbedder {
    private val CHARSET = "UTF-8"

    /**
     * Ajoute une image encodée en base64 à la fin du fichier MP4 sans altérer sa lisibilité.
     */
    override suspend fun appendBase64ToFile(file: File, base64Image: String, tag: Tags) {
        withContext(Dispatchers.IO) {
            val savedScale = extractScaleFromFile(file)
            removeScale(file)
            val savedTag = extractFlagFromFile(file)
            removeFlagFromFile(file)

            if (tag == Tags.COVER) {
                val base64Cropped = extractBase64FromFile(file, 65536, 5, Tags.COVER_CROPPED)
                removeBothBase64(file)

                RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(raf.length())
                    val data = "\n${tag.start}\n$base64Image\n${tag.end}\n"
                    raf.write(data.toByteArray(Charset.forName(CHARSET)))
                }

                if (base64Cropped != null)
                    RandomAccessFile(file, "rw").use { raf ->
                        raf.seek(raf.length())
                        val data =
                            "\n${Tags.COVER_CROPPED.start}\n$base64Cropped\n${Tags.COVER_CROPPED.end}\n"
                        raf.write(data.toByteArray(Charset.forName(CHARSET)))
                    }
                else
                    RandomAccessFile(file, "rw").use { raf ->
                        raf.seek(raf.length())
                        val data = "\n${Tags.COVER_CROPPED.start}\n$base64Image\n{Tags.COVER_CROPPED.end}\n"
                        raf.write(data.toByteArray(Charset.forName(CHARSET)))
                    }
            }

            if (tag == Tags.COVER_CROPPED) {
                RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(raf.length())
                    val data = "\n${Tags.COVER_CROPPED.start}\n$base64Image\n${Tags.COVER_CROPPED.end}\n"
                    raf.write(data.toByteArray(Charset.forName(CHARSET)))
                }
            }

            if (savedScale != null)
                appendScaleToFile(file, savedScale)
            
            if (savedTag != null)
                appendFlagToFile(file, savedTag)
        }
    }

    /**
     * Extrait l'image encodée en base64 si elle a été ajoutée via [appendBase64ToFile].
     * Recherche progressivement dans des blocs de taille croissante si besoin.
     */
    override suspend fun extractBase64FromFile(
        mp4File: File,
        initialLookbackBytes: Int,
        maxAttempts: Int,
        tag: Tags
    ): String? {
        val charset = Charset.forName(CHARSET)
        RandomAccessFile(mp4File, "r").use { raf ->
            val length = raf.length()
            for (attempt in 1..maxAttempts) {
                val lookbackBytes = (initialLookbackBytes * attempt).toLong()
                val seekBack = minOf(lookbackBytes, length)
                raf.seek(length - seekBack)

                val bytes = ByteArray(seekBack.toInt())
                raf.readFully(bytes)

                val tail = String(bytes, charset)
                val startIndex = tail.lastIndexOf(tag.start)
                val endIndex = tail.lastIndexOf(tag.end)

                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    return tail.substring(startIndex + tag.start.length, endIndex).trim()
                }
            }

            if (tag == Tags.COVER_CROPPED) {
                val result =
                    extractBase64FromFile(mp4File, initialLookbackBytes, maxAttempts, Tags.COVER)
                if (result != null) {
                    appendBase64ToFile(mp4File, result, Tags.COVER_CROPPED)
                }

                return result
            }

            return null
        }
    }

    /**
     * Supprime les données base64 insérées (si présentes).
     */
    override suspend fun removeBothBase64(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            val savedScale = extractScaleFromFile(file)
            val savedTag = extractFlagFromFile(file)
            //inutile car plus bas le scale est aussi supprimé de facto
            //removeEmbeddedContentScale(mp4File)

            var result = false
            RandomAccessFile(file, "rw").use { raf ->
                val length = raf.length()
                val lookbackBytes = 65536L * 2
                val seekBack = minOf(lookbackBytes, length)
                raf.seek(length - seekBack)

                val bytes = ByteArray(seekBack.toInt())
                raf.readFully(bytes)

                val tail = String(bytes, Charset.forName(CHARSET))
                //START_BASE64_TAG étant la première des 2 images, ont enlève les 2 d'un coup
                val startIndex = tail.indexOf(Tags.COVER.start)

                if (startIndex != -1) {
                    val absoluteStart = length - seekBack + startIndex
                    raf.setLength(absoluteStart)
                    result = true
                } else {
                    result = false
                }
            }

            if (result) {
                if (savedScale != null) {
                    appendScaleToFile(file, savedScale)
                }
                
                if (savedTag != null) {
                    appendFlagToFile(file, savedTag)    
                }
            }

            result
        }
    }

    override suspend fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun bitmapToBase64(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int
    ): String {
        return withContext(Dispatchers.Main) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }

    override suspend fun appendScaleToFile(file: File, contentScale: ContentScale) {
        withContext(Dispatchers.IO) {
            val savedTag = extractFlagFromFile(file)
            removeFlagFromFile(file)
            
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(raf.length())

                val scaleAsString = when (contentScale) {
                    ContentScale.Crop -> "Crop"
                    ContentScale.Fit -> "Fit"
                    ContentScale.None -> "None"
                    ContentScale.Inside -> "Inside"
                    ContentScale.FillWidth -> "FillWidth"
                    ContentScale.FillHeight -> "FillHeight"
                    ContentScale.FillBounds -> "FillBounds"
                    else -> "Crop"
                }
                val data = "\n${Tags.SCALE.start}\n$scaleAsString\n${Tags.SCALE.end}\n"
                raf.write(data.toByteArray(Charset.forName(CHARSET)))
            }

            if (savedTag != null)
                appendFlagToFile(file, savedTag)
        }
    }

    override suspend fun extractScaleFromFile(file: File): ContentScale? {
        val charset = Charset.forName(CHARSET)
        RandomAccessFile(file, "r").use { raf ->
            val length = raf.length()
            val lookbackBytes = 500L
            val seekBack = minOf(lookbackBytes, length)
            raf.seek(length - seekBack)

            val bytes = ByteArray(seekBack.toInt())
            raf.readFully(bytes)

            val tail = String(bytes, charset)
            val startIndex = tail.lastIndexOf(Tags.SCALE.start)
            val endIndex = tail.lastIndexOf(Tags.SCALE.end)

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                return scaleFromString(
                    tail.substring(startIndex + Tags.SCALE.start.length, endIndex).trim()
                )
            }
            return null
        }
    }

    fun scaleFromString(value: String): ContentScale? = when (value) {
        "Crop" -> ContentScale.Crop
        "Fit" -> ContentScale.Fit
        "FillBounds" -> ContentScale.FillBounds
        "FillHeight" -> ContentScale.FillHeight
        "FillWidth" -> ContentScale.FillWidth
        "Inside" -> ContentScale.Inside
        "None" -> ContentScale.None
        else -> null // ou exception
    }

    override suspend fun removeScale(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            val savedTag = extractFlagFromFile(file)
            
            RandomAccessFile(file, "rw").use { raf ->
                val length = raf.length()
                val lookbackBytes = 500L
                val seekBack = minOf(lookbackBytes, length)
                raf.seek(length - seekBack)

                val bytes = ByteArray(seekBack.toInt())
                raf.readFully(bytes)

                val tail = String(bytes, Charset.forName(CHARSET))
                val startIndex = tail.indexOf(Tags.SCALE.start)

                val result = if (startIndex != -1) {
                    val absoluteStart = length - seekBack + startIndex
                    raf.setLength(absoluteStart)
                    true
                } else {
                    false
                }

                if (result) {
                    if (savedTag != null) {
                        appendFlagToFile(file, savedTag)
                    }
                }

                return@withContext result
            }
        }
    }

    override suspend fun appendFlagToFile(
        file: File,
        flag: ColoredTag
    ) {
        withContext(Dispatchers.IO) {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(raf.length())

                val text = Gson().toJson(flag)

                val data = "\n${Tags.COLORED_TAG.start}\n$text\n${Tags.COLORED_TAG.end}\n"
                raf.write(data.toByteArray(Charset.forName(CHARSET)))
            }
        }
    }

    override suspend fun extractFlagFromFile(file: File): ColoredTag? {
        val charset = Charset.forName(CHARSET)
        RandomAccessFile(file, "r").use { raf ->
            val length = raf.length()
            val lookbackBytes = 500L
            val seekBack = minOf(lookbackBytes, length)
            raf.seek(length - seekBack)

            val bytes = ByteArray(seekBack.toInt())
            raf.readFully(bytes)

            val tail = String(bytes, charset)
            val startIndex = tail.lastIndexOf(Tags.COLORED_TAG.start)
            val endIndex = tail.lastIndexOf(Tags.COLORED_TAG.end)

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                val json =
                    tail.substring(startIndex + Tags.COLORED_TAG.start.length, endIndex).trim()
                return Gson().fromJson(json, ColoredTag::class.java)
            }
            return null
        }
    }

    override suspend fun removeFlagFromFile(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            RandomAccessFile(file, "rw").use { raf ->
                val length = raf.length()
                val lookbackBytes = 500L
                val seekBack = minOf(lookbackBytes, length)
                raf.seek(length - seekBack)

                val bytes = ByteArray(seekBack.toInt())
                raf.readFully(bytes)

                val tail = String(bytes, Charset.forName(CHARSET))
                val startIndex = tail.indexOf(Tags.COLORED_TAG.start)

                if (startIndex != -1) {
                    val absoluteStart = length - seekBack + startIndex
                    raf.setLength(absoluteStart)
                    true
                } else {
                    false
                }
            }
        }
        
        
        
    }

    override suspend fun appendMemoToFile(filePath: String) {
        
        
        
        
    }

    override suspend fun removeMemoFromFile(filePath: String): Boolean {
        return false
    }
}

sealed class Tags(
    val start: String,
    val end: String
) {
    object SCALE : Tags(
        start = "##SIGMA-SCALE-START##",
        end = "##SIGMA-SCALE-END##"
    )

    object COVER : Tags(
        start = "##SIGMA-COVER-START##",
        end = "##SIGMA-COVER-END##"
    )

    object COVER_CROPPED : Tags(
        start = "##SIGMA-COVER_CROPPED-START##",
        end = "##SIGMA-COVER_CROPPED-END##"
    )

    object COLORED_TAG : Tags(
        start = "##SIGMA-TAG-START##",
        end = "##SIGMA-TAG-END##"
    )

    object MEMO : Tags(
        start = "##SIGMA-MEMO-START##",
        end = "##SIGMA-MEMO-END##"
    )
}