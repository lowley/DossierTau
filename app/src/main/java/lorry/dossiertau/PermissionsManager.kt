package lorry.dossiertau

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import androidx.core.content.ContextCompat.startActivity

class PermissionsManager {
    fun hasExternalStoragePermission() : Boolean{
        return Environment.isExternalStorageManager()
    }
    
    fun requestExternalStoragePermission(context: Context) : Boolean{
        val uri = Uri.parse("package:"+context.applicationContext.packageName)
        
        try {
            startActivity(
                context,
                Intent(
                    ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                ), null
            )
        }catch (ex: Exception) {
            return false
        }
        
        return hasExternalStoragePermission()
    }
}