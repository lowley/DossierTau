package lorry.dossiertau.ui.support.capsule.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.ui.support.base64.VideoInfoEmbedder
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileCapsuleManager(
    val targetPath: String,
    val useOld: Boolean
) {
    val targetPathHere = targetPath.toTauPath()

    suspend fun save(element: IElementInCapsule, forFolder: Boolean) {

        val eliminate = forFolder && (element is InitialPicture || element is CroppedPicture)

        if (!eliminate) {
            val capsuleIO = if (useOld)
                FileCapsuleIO()
            else FileMetadataManager()

            val target = targetPathHere.toFile().getOrNull()
            val existingCapsule = if (target?.exists() == true) {
                capsuleIO.getCapsule(targetPathHere.path) ?: CapsuleData()
            } else {
                CapsuleData()
            }

            val updatedCapsule = element.update(existingCapsule)
            capsuleIO.replaceCapsule(targetPathHere.path, updatedCapsule)
        }

        //branchement enregistrement en fichier pour dossiers
        val pictureDir = File(targetPathHere.path.replaceAfterLast("/", ".sigma"))
        if (!pictureDir.exists())
            pictureDir.mkdirs()

        if (forFolder && element is InitialPicture && element.initialPicture != null) {
            val file = File(targetPathHere.path.replaceAfterLast("/", ".sigma/initialPicture.webp"))

            FileOutputStream(file).use<FileOutputStream, Unit> { out ->
                (element.initialPicture as Bitmap).compress(
                    Bitmap.CompressFormat.WEBP_LOSSLESS, // or WEBP_LOSSY
                    100, out
                )
            }
        }

        if (forFolder && element is CroppedPicture && element.croppedPicture != null) {
            val file = File(targetPathHere.path.replaceAfterLast("/", ".sigma/croppedPicture.webp"))

            FileOutputStream(file).use<FileOutputStream, Unit> { out ->
                (element.croppedPicture as Bitmap).compress(
                    Bitmap.CompressFormat.WEBP_LOSSLESS, // or WEBP_LOSSY
                    100, out
                )
            }
        }
    }

    suspend fun getCapsule(): CapsuleData {
        val compositeIO = FileMetadataManager()

        val target = targetPathHere.toFile().getOrNull()
        val firstMelt = withContext(Dispatchers.IO) {
            if (target?.exists() == true) {
                compositeIO.getCapsule(targetPathHere.path) ?: CapsuleData()
            } else
                CapsuleData()
        }

        val folderSuffix = ".folderPicture.html"
        if (targetPathHere?.path?.endsWith(folderSuffix) != true)
            return firstMelt

        val initialWebp = File(targetPathHere.path
            .substringBeforeLast("/")
            .plus("/.sigma/initialPicture.webp")
        )

        val initialWebpExists = initialWebp.exists()
        val secondMeltInitial = if (initialWebpExists) try {
            var result: String? = null
            FileInputStream(initialWebp).use{ inputStream ->
                BufferedInputStream(inputStream).use{ bufferedInputStream ->
                    val bmp = BitmapFactory.decodeStream(bufferedInputStream)
                    result = if (bmp != null) {
                        val videoInfoEmbedder = VideoInfoEmbedder()
                        val b64 = videoInfoEmbedder.bitmapToBase64(bmp)
                        b64
                    } else null

                    result
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } else null

        val croppedWebp = File(targetPathHere.path
            .substringBeforeLast("/")
            .plus("/.sigma/croppedPicture.webp")
        )

        val croppedWebpExists = croppedWebp.exists()
        val secondMeltCropped = if (croppedWebpExists) try {
            var result: String?
            FileInputStream(croppedWebp).use{ inputStream ->
                BufferedInputStream(inputStream).use{ bufferedInputStream ->
                    val bmp = BitmapFactory.decodeStream(bufferedInputStream)
                    result = if (bmp != null) {
                        val videoInfoEmbedder = VideoInfoEmbedder()
                        val b64 = videoInfoEmbedder.bitmapToBase64(bmp)
                        b64
                    } else null

                    result
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        else null

        if (secondMeltInitial != null || secondMeltCropped != null) {
            return firstMelt.copy(
                initialPicture = secondMeltInitial,
                croppedPicture = secondMeltCropped

            )
        }

        if (targetPathHere.path.startsWith("adrilu"))
            println("ok")

        return firstMelt
    }

    suspend fun <T> getElement(reader: IElementReader<T>): T? {
        return withContext(Dispatchers.IO) {
            reader.fileGet(targetPathHere)
        }
    }
}