package lorry.dossiertau

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import lorry.dossiertau.data.intelligenceService.CIA

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, CIA::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}