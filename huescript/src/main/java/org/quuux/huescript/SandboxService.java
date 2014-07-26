package org.quuux.huescript;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.renderscript.Script;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SandboxService extends Service {

    private static final String TAG = Log.buildTag(SandboxService.class);

    public static final String ACTION_SCRIPTS_UPDATED = "org.quuux.huescript.intents.SCRIPTS_UPDATED";

    private static final int COMMAND_SCAN_DIR = 0x1;
    private static final int COMMAND_RUN_SCRIPT = 0x2;
    private static final int COMMAND_SCRIPT_ADDED = 0x3;
    private static final int COMMAND_SCRIPT_REMOVED = 0x4;
    private static final int COMMAND_SCRIPT_FINISHED = 0x5;
    private static final int COMMAND_RELOAD_SCRIPT = 0x6;

    private static final File SCRIPTS_DIR = new File(Environment.getExternalStorageDirectory(), "HueScripts");
    private FileObserver mObserver;
    private HandlerThread mHandlerThread;
    private ServiceHandler mHandler;

    private List<Sandbox> mScripts = new CopyOnWriteArrayList<Sandbox>();
    private List<Sandbox> mRunningScripts = new CopyOnWriteArrayList<Sandbox>();

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        mHandlerThread = new HandlerThread("ServiceHandler");
        mHandlerThread.start();

        mHandler = new ServiceHandler(mHandlerThread.getLooper());

        mObserver = new ScriptsObserver(SCRIPTS_DIR.getAbsolutePath());
        mObserver.startWatching();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Sandbox.ACTION_CALL_START);
        filter.addAction(Sandbox.ACTION_CALL_END);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mObserver.stopWatching();
        unregisterReceiver(mReceiver);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {

        scanDir(SCRIPTS_DIR);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    public void scanDir(final File dir) {
        mHandler.sendMessage(mHandler.obtainMessage(COMMAND_SCAN_DIR, dir));
    }

    public List<Sandbox> getScripts() {
        return mScripts;
    }

    public void runSandbox(final Sandbox sandbox) {
        mHandler.sendMessage(mHandler.obtainMessage(COMMAND_RUN_SCRIPT, sandbox));
    }

    public void doRunSandbox(final Sandbox sandbox) {
        if (!mRunningScripts.contains(sandbox)) {
            mRunningScripts.add(sandbox);
            sandbox.callExport(this, "main", this);
        }
    }

    private boolean doScanDir(final File dir) {
        Log.d(TAG, "loading scripts from %s", dir.getAbsolutePath());

        boolean added = false;
        for (final File f : dir.listFiles()) {
            if (f.isDirectory() && new File(f, "main.js").exists()) {
                loadScript(f);
                added = true;
            }
        }

        return added;
    }

    private Sandbox loadScript(final File path) {
        Log.d(TAG, "loading script %s", path.getAbsolutePath());
        final Sandbox sandbox = new Sandbox(path);
        sandbox.require();
        mScripts.add(sandbox);
        return sandbox;
    }
    
    private void sendLocalBroadcast(final Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);        
    } 
    
    private void broadcastUpdate() {
        final Intent intent = new Intent(ACTION_SCRIPTS_UPDATED);
        sendLocalBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        SandboxService getService() {
            return SandboxService.this;
        }
    }

    private class ServiceHandler extends Handler {

        public ServiceHandler(final Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {

                case COMMAND_SCAN_DIR:
                    if (doScanDir((File) msg.obj))
                        broadcastUpdate();
                    break;

                case COMMAND_RUN_SCRIPT:
                    doRunSandbox((Sandbox) msg.obj);
                    break;

                case COMMAND_SCRIPT_ADDED:
                    doScriptAdded((File)msg.obj);
                    break;

                case COMMAND_SCRIPT_REMOVED:
                    if (doScriptRemoved((File)msg.obj))
                        broadcastUpdate();
                    break;

                case COMMAND_SCRIPT_FINISHED:
                    doScriptFinished((String) msg.obj);
                    break;

                case COMMAND_RELOAD_SCRIPT:
                    if (doReloadScript((File)msg.obj))
                        broadcastUpdate();
                    break;

            }

        }
    }

    private boolean doReloadScript(final File path) {
        doScriptRemoved(path);
        doScriptAdded(path);
        return true;
    }

    private Sandbox getScript(final File path) {
        for (Sandbox script : mScripts) {
            if (script.getPath().equals(path)) {
                return script;
            }
        }

        return null;
    }

    private boolean doScriptRemoved(final File path) {
        Sandbox script = getScript(path);
        if (script != null) {
            mScripts.remove(script);
        }

        return script != null;
    }

    private boolean doScriptAdded(final File path) {
        Sandbox script = getScript(path);
        boolean exists = script != null;
        if (!exists) {
            script = new Sandbox(path);
            script.require();
            mScripts.add(script);
        }

        return !exists;
    }

    private class ScriptsObserver extends FileObserver {

        public ScriptsObserver(final String path) {
            super(path,
                    FileObserver.ALL_EVENTS
            );
        }

        @Override
        public void onEvent(final int event, final String path) {
            if (path == null)
                return;

            final File file = new File(SCRIPTS_DIR.getAbsolutePath(), path);
            Log.d(TAG, "path: %s / file: %s / event: %s", path, file, event & FileObserver.ALL_EVENTS);

            switch (event & FileObserver.ALL_EVENTS) {
                case FileObserver.ATTRIB:
                    reloadScript(file);
                    break;

                case FileObserver.DELETE:
                case FileObserver.MOVED_FROM:
                    scriptRemoved(file);
                    break;

            }

        }
    }

    private void reloadScript(final File file) {
        mHandler.sendMessage(mHandler.obtainMessage(COMMAND_RELOAD_SCRIPT, file));
    }

    private void scriptAdded(final File file) {
        mHandler.sendMessage(mHandler.obtainMessage(COMMAND_SCRIPT_ADDED, file));
    }

    private void scriptRemoved(final File file) {
        mHandler.sendMessage(mHandler.obtainMessage(COMMAND_SCRIPT_REMOVED, file));
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "broadcast: %s", intent);

            if (Sandbox.ACTION_CALL_END.equals(intent.getAction())) {
                scriptFinished(intent.getStringExtra("name"));
            }
        }
    };

    private void scriptFinished(final String name) {
        mHandler.sendMessage(mHandler.obtainMessage(COMMAND_SCRIPT_FINISHED));
    }

    private void doScriptFinished(final String name) {
        Sandbox found = null;
        for (final Sandbox script : mRunningScripts) {
            if (script.getName().equals(name)) {
                found = script;
                break;
            }
        }

        if (found != null) {
            mRunningScripts.remove(found);
        }
    }

}
