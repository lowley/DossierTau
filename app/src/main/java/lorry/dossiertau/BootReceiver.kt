package lorry.dossiertau

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Android récents interdisent le démarrage d'un foreground service dataSync
 * directement depuis BOOT_COMPLETED. La CIA est démarrée lorsque DossierTau
 * est réellement ouvert/utilisé, ce qui évite ForegroundServiceStartNotAllowedException.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("DossierTauBoot", "BOOT_COMPLETED reçu; démarrage CIA différé jusqu'à l'ouverture de DossierTau")
        }
    }
}
