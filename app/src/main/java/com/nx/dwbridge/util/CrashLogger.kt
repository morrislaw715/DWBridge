package com.nx.dwbridge.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashLogger {
    private const val TAG = "DWBridgeCrash"
    private const val FILENAME_PREFIX = "crash_"
    private val DATE_FMT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val ts = DATE_FMT.format(Date())
                val fileName = "$FILENAME_PREFIX$ts.txt"
                val file = File(context.filesDir, fileName)
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                pw.println("Thread: ${thread.name}")
                throwable.printStackTrace(pw)
                pw.flush()
                val content = sw.toString()
                file.writeText(content)
                Log.e(TAG, "Wrote crash to ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash log", e)
            } finally {
                // Pass to the original handler (this will terminate the app)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Return list of crash files (most recent first)
     */
    fun listCrashFiles(context: Context): List<File> {
        val dir = context.filesDir
        return dir.listFiles { f -> f.name.startsWith(FILENAME_PREFIX) }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun readCrashFile(file: File): String = try {
        file.readText()
    } catch (e: Exception) {
        "Failed to read: ${e.message}"
    }
}

