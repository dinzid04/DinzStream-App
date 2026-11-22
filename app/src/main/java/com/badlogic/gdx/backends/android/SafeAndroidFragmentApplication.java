package com.badlogic.gdx.backends.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.SnapshotArray;

/**
 * Host LibGDX berbasis AndroidX Fragment yang sepenuhnya aman di Android 14/15/16 (One UI):
 * - Memakai SafeAndroidGraphics yang TIDAK PERNAH memanggil Process.killProcess() saat pause deadlock
 *   dan TIDAK PERNAH menunggu tanpa batas di destroy().
 * - Selalu memanggil super.onPause()/super.onResume() (Fragment) dengan null-guard penuh.
 * - Tidak memakai deprecated AndroidVisibilityListener (mode fullscreen dikelola WindowInsetsControllerCompat).
 */
public class SafeAndroidFragmentApplication extends Fragment implements AndroidApplicationBase {

    public interface Callbacks {
        void exit();
    }

    private static final String TAG = "SafeGdxFragment";

    protected SafeAndroidGraphics graphics;
    protected AndroidInput input;
    protected AndroidAudio audio;
    protected AndroidFiles files;
    protected AndroidNet net;
    protected AndroidClipboard clipboard;
    protected ApplicationListener listener;
    public Handler handler;
    protected boolean firstResume = true;
    protected boolean gdxInitialized = false;
    protected final Array<Runnable> runnables = new Array<Runnable>();
    protected final Array<Runnable> executedRunnables = new Array<Runnable>();
    protected final SnapshotArray<LifecycleListener> lifecycleListeners =
            new SnapshotArray<LifecycleListener>(LifecycleListener.class);
    protected int logLevel = LOG_INFO;
    protected ApplicationLogger applicationLogger;
    protected Callbacks callbacks;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Callbacks) {
            this.callbacks = (Callbacks) context;
        } else if (getParentFragment() instanceof Callbacks) {
            this.callbacks = (Callbacks) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.callbacks = null;
    }

    public View initializeForView(ApplicationListener listener, AndroidApplicationConfiguration config) {
        if (this.getVersion() < MINIMUM_SDK) {
            throw new GdxRuntimeException("libGDX requires Android API Level " + MINIMUM_SDK + " or later.");
        }
        Activity hostActivity = getActivity();
        if (hostActivity == null) {
            throw new GdxRuntimeException("Host Activity tidak tersedia saat inisialisasi LibGDX.");
        }
        GdxNativesLoader.load();
        setApplicationLogger(new AndroidApplicationLogger());

        ResolutionStrategy strategy = config.resolutionStrategy != null
                ? config.resolutionStrategy
                : new FillResolutionStrategy();
        graphics = new SafeAndroidGraphics(this, config, strategy);
        input = createInput(this, hostActivity, graphics.view, config);
        audio = createAudio(hostActivity, config);
        files = createFiles();
        net = new AndroidNet(this, config);
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        this.clipboard = new AndroidClipboard(hostActivity);

        addLifecycleListener(new LifecycleListener() {
            @Override
            public void resume() {
                try {
                    if (audio != null) audio.resume();
                } catch (Throwable ignored) {}
            }

            @Override
            public void pause() {
                try {
                    if (audio != null) audio.pause();
                } catch (Throwable ignored) {}
            }

            @Override
            public void dispose() {
                try {
                    if (audio != null) audio.dispose();
                } catch (Throwable ignored) {}
            }
        });

        Gdx.app = this;
        Gdx.input = this.input;
        Gdx.audio = this.audio;
        Gdx.files = this.files;
        Gdx.graphics = this.graphics;
        Gdx.net = this.net;

        if (config.useWakelock) {
            try {
                hostActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } catch (Throwable ignored) {}
        }

        if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS) {
            input.setKeyboardAvailable(true);
        }

        gdxInitialized = true;
        return graphics.getView();
    }

    @Override
    public void onPause() {
        if (gdxInitialized && graphics != null && input != null) {
            try {
                boolean isContinuous = graphics.isContinuousRendering();
                boolean isContinuousEnforced = AndroidGraphics.enforceContinuousRendering;

                AndroidGraphics.enforceContinuousRendering = true;
                graphics.setContinuousRendering(true);
                graphics.pause();
                input.onPause();

                Activity act = getActivity();
                if (isRemoving() || isAnyParentFragmentRemoving() || (act != null && act.isFinishing())) {
                    graphics.clearManagedCaches();
                    graphics.destroy();
                }

                AndroidGraphics.enforceContinuousRendering = isContinuousEnforced;
                graphics.setContinuousRendering(isContinuous);
                graphics.onPauseGLSurfaceView();
            } catch (Throwable t) {
                Log.w(TAG, "Safely handled error during onPause: " + t.getMessage(), t);
            }
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (gdxInitialized && graphics != null && input != null) {
            try {
                Gdx.app = this;
                Gdx.input = this.input;
                Gdx.audio = this.audio;
                Gdx.files = this.files;
                Gdx.graphics = this.graphics;
                Gdx.net = this.net;

                input.onResume();
                graphics.onResumeGLSurfaceView();

                if (!firstResume) {
                    graphics.resume();
                } else {
                    firstResume = false;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Safely handled error during onResume: " + t.getMessage(), t);
            }
        }
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration config) {
        super.onConfigurationChanged(config);
        if (input != null) {
            boolean keyboardAvailable = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            input.setKeyboardAvailable(keyboardAvailable);
        }
    }

    @Override
    public ApplicationListener getApplicationListener() {
        return listener;
    }

    @Override
    public Audio getAudio() {
        return audio;
    }

    @Override
    public Files getFiles() {
        return files;
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public AndroidInput getInput() {
        return input;
    }

    @Override
    public Net getNet() {
        return net;
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.Android;
    }

    @Override
    public int getVersion() {
        return Build.VERSION.SDK_INT;
    }

    @Override
    public long getJavaHeap() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap() {
        return Debug.getNativeHeapAllocatedSize();
    }

    @Override
    public Preferences getPreferences(String name) {
        Context ctx = getContext();
        return new AndroidPreferences(ctx.getSharedPreferences(name, Context.MODE_PRIVATE));
    }

    @Override
    public Clipboard getClipboard() {
        return clipboard;
    }

    @Override
    public void postRunnable(Runnable runnable) {
        if (runnable == null) return;
        synchronized (runnables) {
            runnables.add(runnable);
            if (graphics != null) {
                graphics.requestRendering();
            }
        }
    }

    @Override
    public void exit() {
        Handler h = this.handler;
        if (h != null) {
            h.post(() -> {
                if (callbacks != null) {
                    callbacks.exit();
                }
            });
        } else if (callbacks != null) {
            callbacks.exit();
        }
    }

    @Override
    public void debug(String tag, String message) {
        if (logLevel >= LOG_DEBUG) Log.d(tag, message);
    }

    @Override
    public void debug(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_DEBUG) Log.d(tag, message, exception);
    }

    @Override
    public void log(String tag, String message) {
        if (logLevel >= LOG_INFO) Log.i(tag, message);
    }

    @Override
    public void log(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_INFO) Log.i(tag, message, exception);
    }

    @Override
    public void error(String tag, String message) {
        if (logLevel >= LOG_ERROR) Log.e(tag, message);
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        if (logLevel >= LOG_ERROR) Log.e(tag, message, exception);
    }

    @Override
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public int getLogLevel() {
        return logLevel;
    }

    @Override
    public void setApplicationLogger(ApplicationLogger applicationLogger) {
        this.applicationLogger = applicationLogger;
    }

    @Override
    public ApplicationLogger getApplicationLogger() {
        return applicationLogger;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.removeValue(listener, true);
        }
    }

    @Override
    public Context getContext() {
        Activity act = getActivity();
        return act != null ? act : super.getContext();
    }

    @Override
    public Array<Runnable> getRunnables() {
        return runnables;
    }

    @Override
    public Array<Runnable> getExecutedRunnables() {
        return executedRunnables;
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        Activity act = getActivity();
        if (act != null) {
            act.runOnUiThread(runnable);
        } else if (handler != null) {
            handler.post(runnable);
        }
    }

    @Override
    public SnapshotArray<LifecycleListener> getLifecycleListeners() {
        return lifecycleListeners;
    }

    @Override
    public Window getApplicationWindow() {
        Activity act = getActivity();
        return act != null ? act.getWindow() : null;
    }

    @Override
    public WindowManager getWindowManager() {
        Context ctx = getContext();
        return ctx != null ? (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE) : null;
    }

    @Override
    public void useImmersiveMode(boolean b) {
        // Dikelola modern via WindowInsetsControllerCompat di RpgGameActivity
    }

    @Override
    public Handler getHandler() {
        return this.handler;
    }

    @Override
    public AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config) {
        if (!config.disableAudio) {
            return new DefaultAndroidAudio(context, config);
        }
        return new DisabledAndroidAudio();
    }

    @Override
    public AndroidInput createInput(Application activity, Context context, Object view, AndroidApplicationConfiguration config) {
        return new DefaultAndroidInput(this, context, view, config);
    }

    protected AndroidFiles createFiles() {
        return new DefaultAndroidFiles(getResources().getAssets(), getActivity(), true);
    }

    private boolean isAnyParentFragmentRemoving() {
        Fragment parent = getParentFragment();
        while (parent != null) {
            if (parent.isRemoving()) return true;
            parent = parent.getParentFragment();
        }
        return false;
    }
}
