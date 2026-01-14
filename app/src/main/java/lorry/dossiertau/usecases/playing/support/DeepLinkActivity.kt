package lorry.dossiertau.usecases.playing.support

// package com.votre.package
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeepLinkActivity : ComponentActivity() {


    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_VIEW && intent?.data != null) {
            val (videoUri, readerPackage, title) = analyzeIntent(intent)
            startVlcOnce(videoUri, readerPackage, title, this)

        }

        lifecycleScope.launch {
            // 50–150 ms suffit en général ; évite de terminer "trop tôt"
            delay(120)
            // Variante A (douce) :
            finish()
            // Variante B (si tu veux que la tâche disparaisse du récents) :
            // finishAndRemoveTask()
            overridePendingTransition(0, 0)
        }
    }

    private fun analyzeIntent(intent: Intent):
            Triple<VideoUriType, VideoPlayerPackageType, VideoTitleType> {

        val data = intent.dataString
        if (data.isNullOrEmpty())
            return Triple(
                VideoUriType.EMPTY,
                VideoPlayerPackageType.EMPTY,
                VideoTitleType.EMPTY
            )

        val PLAYER_PATTERN = "?player="
        val VIDEO_PATTERN = "?video="

        val player = data.substringAfter(PLAYER_PATTERN)
        if (player.isEmpty())
            return Triple(
                VideoUriType.EMPTY,
                VideoPlayerPackageType.EMPTY,
                VideoTitleType.EMPTY
            )

        val readerPackage = when (player) {
            "vlc" -> "org.videolan.vlc"
            "bsplayer" -> "com.bsplayer.bspandroid.free"
            else -> "org.videolan.vlc"
        }

        val beginning = data.substringBefore(PLAYER_PATTERN)
        val videoUrl = beginning.substringAfter(VIDEO_PATTERN)

        if (videoUrl.isEmpty())
            return Triple(
                VideoUriType.EMPTY,
                VideoPlayerPackageType.EMPTY,
                VideoTitleType.EMPTY
            )

        val title = videoUrl.substringAfter("videos/")
        if (title.isEmpty())
            return Triple(
                VideoUriType.EMPTY,
                VideoPlayerPackageType.EMPTY,
                VideoTitleType.EMPTY
            )

        return Triple(
            VideoUriType(videoUrl),
            VideoPlayerPackageType(readerPackage),
            VideoTitleType(title)
        )
    }

    companion object {

        fun startVlcOnce(
            videoUri: VideoUriType,
            readerPackage: VideoPlayerPackageType,
            title: VideoTitleType,
            activity: Activity
        ) {

            val videoUriValue = videoUri.value
            val readerPackageValue = readerPackage.value
            val titleValue = title.value
            if (videoUriValue.isEmpty())
                return

            val videoUri: Uri = videoUriValue.toUri()
            val launchPlayerIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/*")
                putExtra("title", titleValue)
                putExtra(Intent.EXTRA_TITLE, titleValue)

                //launchPlayerIntent.SetPackage("org.videolan.vlc");
                //launchPlayerIntent.SetPackage("com.bsplayer.bspandroid.free");
                setPackage(readerPackageValue);
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                activity.startActivity(launchPlayerIntent)
            } catch (e: ActivityNotFoundException) {
                // TODO: informer l’utilisateur (VLC absent) puis finish()
                println("Dossier Sigma, lancement d'un vidéo demandé mais VLC non trouvé")
            }
        }
    }
}

@JvmInline
value class VideoUriType(val value: String) {
    companion object {
        val EMPTY = VideoUriType("")
    }
}

@JvmInline
value class VideoPlayerPackageType(val value: String) {
    companion object {
        val EMPTY = VideoPlayerPackageType("")
    }
}

@JvmInline
value class VideoTitleType(val value: String) {
    companion object {
        val EMPTY = VideoTitleType("")
    }
}
