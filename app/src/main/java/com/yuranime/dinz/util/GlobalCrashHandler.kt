package com.yuranime.dinz.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.Process
import android.util.Log
import com.yuranime.dinz.MainActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.system.exitProcess

/**
 * Penangkap exception global agar error tak terduga pada thread latar atau komponen UI
 * tidak langsung menyebabkan force-close berulang tanpa pemulihan.
 */
object GlobalCrashHandler {
    private const val TAG = "YuranimeCrashHandler"
    private const val PREF_NAME = "yuranime_crash_prefs"
    private const val KEY_LAST_CRASH_MS = "last_crash_ms"
    private const val KEY_LAST_CRASH_TRACE = "last_crash_trace"
    private const val CRASH_LOOP_WINDOW_MS = 8_000L

    @Volatile
    private var installed = false

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception: ${throwable.message}", throwable)
    }

    fun install(app: Application) {
        if (installed) return
        installed = true

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = runCatching {
                Log.getStackTraceString(throwable).take(4000)
            }.getOrDefault(throwable.toString())

            Log.e(TAG, "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            recordCrashTrace(app, trace)

            val isMainThread = runCatching {
                thread == Looper.getMainLooper().thread
            }.getOrDefault(false)

            // Exception fatal VM / OOM pada main thread tidak aman ditelan mentah-mentah
            val isVmFatal = throwable is VirtualMachineError

            if (!isMainThread && !isVmFatal) {
                // Background worker error dicatat aman tanpa membunuh seluruh aplikasi
                return@setDefaultUncaughtExceptionHandler
            }

            if (isMainThread && !isVmFatal && !isInCrashLoop(app)) {
                markCrashTimestamp(app)
                val restarted = runCatching {
                    val intent = Intent(app, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        )
                        putExtra("recovered_from_crash", true)
                    }
                    app.startActivity(intent)
                    true
                }.getOrDefault(false)

                if (restarted) {
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                    return@setDefaultUncaughtExceptionHandler
                }
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun isInCrashLoop(context: Context): Boolean {
        val lastMs = runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_CRASH_MS, 0L)
        }.getOrDefault(0L)
        return lastMs > 0L && (System.currentTimeMillis() - lastMs) < CRASH_LOOP_WINDOW_MS
    }

    private fun markCrashTimestamp(context: Context) {
        runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_CRASH_MS, System.currentTimeMillis())
                .apply()
        }
    }

    private fun recordCrashTrace(context: Context, trace: String) {
        runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_CRASH_TRACE, trace)
                .apply()
        }
    }
}
