package app.orchard.tv.media

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/** Read-only, signature-protected export, invoked only by the player's explicit Import action. */
class LegacyMediaProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "export_live_v1") return null
        val app = context ?: return null
        app.enforceCallingPermission(MediaContract.MIGRATION_PERMISSION, "matching companion signature required")
        return Bundle().apply {
            putInt("version", MediaContract.VERSION)
            putString("configs", app.getSharedPreferences("live_sources", 0).getString("configs", "[]"))
        }
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
