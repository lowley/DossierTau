package lorry.folder.items.dossiersigma.external.playing

import android.app.Activity
import lorry.dossiertau.support.littleClasses.TauPath

interface IPlayingDataSource {

    suspend fun playFile(fullPath: TauPath, type: String, activity: Activity)


}