package com.bazaarhelper.app.core.security

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.bazaarhelper.app.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupManager(private val context: Context) {

    private val dbName = "BazaarHelper.db"

    private fun performCheckpoint() {
        try {
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) {
                // Trigger a checkpoint to ensure all data is in the main .db file
                val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
                db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                db.close()
                Log.d("BACKUP", "Checkpoint successful")
            }
        } catch (e: Exception) {
            Log.e("BACKUP", "Checkpoint failed", e)
        }
    }

    fun exportBackup() {
        try {
            performCheckpoint()
            Log.d("BACKUP_EXPORT", "Starting backup export")
            
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) return

            val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
            val backupFile = File(backupDir, "Bazaar_Backup.db")
            if (backupFile.exists()) backupFile.delete()
            
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", backupFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, context.getString(R.string.save_backup_title)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("BACKUP_EXPORT", "Error during export", e)
        }
    }

    fun importBackup(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            Log.d("BACKUP_IMPORT", "Importing from: $uri")
            val dbFile = context.getDatabasePath(dbName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Delete WAL files to force DB to reload from main file
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            
            onSuccess()
        } catch (e: Exception) {
            Log.e("BACKUP_IMPORT", "Error during import", e)
            onError(e.message ?: context.getString(R.string.unknown_error))
        }
    }
}
