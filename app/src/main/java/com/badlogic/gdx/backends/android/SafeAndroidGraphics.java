package com.badlogic.gdx.backends.android;

import android.util.Log;

import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;

/**
 * AndroidGraphics aman untuk Android 14/15/16 (termasuk Samsung One UI):
 * Mencegah deadlock `synch.wait(4000) -> Process.killProcess(myPid())` pada pause()
 * dan mencegah infinite `synch.wait()` pada destroy() jika Surface sudah dilepas oleh OS.
 */
public class SafeAndroidGraphics extends AndroidGraphics {

    private static final String TAG = "SafeAndroidGraphics";
    private static final long SYNC_TIMEOUT_MS = 700L;

    public SafeAndroidGraphics(
            AndroidApplicationBase application,
            AndroidApplicationConfiguration config,
            ResolutionStrategy resolutionStrategy
    ) {
        super(application, config, resolutionStrategy);
    }

    @Override
    void pause() {
        synchronized (synch) {
            if (!running) return;
            running = false;
            pause = true;

            boolean surfaceValid = false;
            try {
                surfaceValid = view != null && view.getHolder() != null
                        && view.getHolder().getSurface() != null
                        && view.getHolder().getSurface().isValid();
            } catch (Throwable ignored) {}

            if (!surfaceValid) {
                pause = false;
                try {
                    if (app != null && app.getApplicationListener() != null) {
                        app.getApplicationListener().pause();
                    }
                } catch (Throwable ignored) {}
                return;
            }

            try {
                view.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        if (!pause) return;
                        try {
                            onDrawFrame(null);
                        } catch (Throwable ignored) {
                            synchronized (synch) {
                                pause = false;
                                synch.notifyAll();
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                pause = false;
                return;
            }

            long deadline = System.currentTimeMillis() + SYNC_TIMEOUT_MS;
            while (pause) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0L) {
                    Log.w(TAG, "Pause sync timeout; melepas lock secara aman tanpa killProcess.");
                    pause = false;
                    try {
                        if (app != null && app.getApplicationListener() != null) {
                            app.getApplicationListener().pause();
                        }
                    } catch (Throwable ignored) {}
                    break;
                }
                try {
                    synch.wait(remaining);
                } catch (InterruptedException ignored) {
                    pause = false;
                    break;
                }
            }
        }
    }

    @Override
    void destroy() {
        synchronized (synch) {
            running = false;
            if (!created) {
                destroy = false;
                return;
            }
            destroy = true;

            boolean surfaceValid = false;
            try {
                surfaceValid = view != null && view.getHolder() != null
                        && view.getHolder().getSurface() != null
                        && view.getHolder().getSurface().isValid();
            } catch (Throwable ignored) {}

            if (!surfaceValid) {
                destroy = false;
                try {
                    if (app != null && app.getApplicationListener() != null) {
                        app.getApplicationListener().dispose();
                    }
                } catch (Throwable ignored) {}
                return;
            }

            try {
                view.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        if (!destroy) return;
                        try {
                            onDrawFrame(null);
                        } catch (Throwable ignored) {
                            synchronized (synch) {
                                destroy = false;
                                synch.notifyAll();
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                destroy = false;
                try {
                    if (app != null && app.getApplicationListener() != null) {
                        app.getApplicationListener().dispose();
                    }
                } catch (Throwable ignored) {}
                return;
            }

            long deadline = System.currentTimeMillis() + SYNC_TIMEOUT_MS;
            while (destroy) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0L) {
                    Log.w(TAG, "Destroy sync timeout; membersihkan listener secara aman.");
                    destroy = false;
                    try {
                        if (app != null && app.getApplicationListener() != null) {
                            app.getApplicationListener().dispose();
                        }
                    } catch (Throwable ignored) {}
                    break;
                }
                try {
                    synch.wait(remaining);
                } catch (InterruptedException ignored) {
                    destroy = false;
                    break;
                }
            }
        }
    }
}
