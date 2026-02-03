package com.typeassist.app.utils

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Locale

object XiaomiUtils {

    fun isXiaomi(): Boolean {
        return Build.MANUFACTURER.lowercase(Locale.ROOT) == "xiaomi"
    }

    fun hasBackgroundStartPermission(context: Context): Boolean {
        if (!isXiaomi()) return true
        
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            val op = 10021
            val method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow", 
                Int::class.javaPrimitiveType, 
                Int::class.javaPrimitiveType, 
                String::class.java
            )
            val result = method.invoke(appOps, op, Process.myUid(), context.packageName) as Int
            result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Settings.canDrawOverlays(context)
        }
    }

    fun openPermissionSettings(context: Context) {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
        intent.putExtra("extra_pkgname", context.packageName)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            settingsIntent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(settingsIntent)
        }
    }

    fun openAutostartSettings(context: Context) {
        val intent = Intent()
        intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            settingsIntent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(settingsIntent)
        }
    }
}
