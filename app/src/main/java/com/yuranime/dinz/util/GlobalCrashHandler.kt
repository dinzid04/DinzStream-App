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
 * Penangkap exception global dengan proteksi anti crash-loop agar aplikasi tidak pernah
 * keluar-masuk berulang saat terjadi error fatal pada komponen tertentu.
 */
object GlobalCrashHandler {
    private const val TAG = "YuranimeCrashHandler"
    private const val PREF_NAME = "yuranime_crash_prefs"
    private const val KEY_LAST_CRASH_MS = "last_crash_ms"
    private const val KEY_CRASH_STREAK = "crash_streak"
    private const val KEY_LAST_CRASH_TRACE = "last_crash_trace"
    private const val CRASH_LOOP_WINDOW_MS = 20_000L

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

            val isVmFatal = throwable is VirtualMachineError

            if (!isMainThread && !isVmFatal) {
                return@setDefaultUncaughtExceptionHandler
            }

            // Jangan pernah melakukan auto-relaunch jika sudah pernah crash dalam 20 detik terakhir
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

    fun getLastCrashTrace(context: Context): String {
        return runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_CRASH_TRACE, "").orEmpty()
        }.getOrDefault("")
    }

    private fun isInCrashLoop(context: Context): Boolean {
        val prefs = runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }.getOrNull() ?: return true
        val lastMs = prefs.getLong(KEY_LAST_CRASH_MS, 0L)
        val streak = prefs.getInt(KEY_CRASH_STREAK, 0)
        val withinWindow = lastMs > 0L && (System.currentTimeMillis() - lastMs) < CRASH_LOOP_WINDOW_MS
        return withinWindow || streak >= 2
    }

    private fun markCrashTimestamp(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val lastMs = prefs.getLong(KEY_LAST_CRASH_MS, 0L)
            val newStreak = if (lastMs > 0L && (now - lastMs) < 60_000L) {
                prefs.getInt(KEY_CRASH_STREAK, 0) + 1
            } else {
                1
            }
            prefs.edit()
                .putLong(KEY_LAST_CRASH_MS, now)
                .putInt(KEY_CRASH_STREAK, newStreak)
                .commit()
        }
    }

    fun resetCrashStreak(context: Context) {
        runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_CRASH_STREAK, 0)
                .apply()
        }
    }

    private fun recordCrashTrace(context: Context, trace: String) {
        runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_CRASH_TRACE, trace)
                .commit()
        }
    }
}
