package org.quuux.huescript;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SandboxService extends Service {

    private static final String TAG = Log.buildTag(SandboxService.class);

    public static final String ACTION_SCRIPTS_UPDATED = "org.quuux.huescript.intents.SCRIPTS_UPDATED";

    private static final int COMMAND_SCAN_DIR = 0x1;
    private static final int COMMAND_SCRIPT_CHANGED = 0x2;

    private static final File SCRIPTS_DIR = new File(Environment.getExternalStorageDirectory(), "HueScripts");

    private FileObserver mObserver;
    private HandlerThread mHandlerThread;
    private ServiceHandler mHandler;

    private List<Sandbox> mScripts = new CopyOnWriteArrayList<Sandbox>();
    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        mHandlerThread = new HandlerThread("ServiceHandler");
        mHandlerThread.start();

        mHandler = new ServiceHandler(mHandlerThread.getLooper());

        mObserver = new ScriptsObserver(SCRIPTS_DIR.getAbsolutePath());
        mObserver.startWatching();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mObserver.stopWatching();
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
                    if (doScanDir((File) msg.obj)) {
                        broadcastUpdate();
                    }
                    break;

                case COMMAND_SCRIPT_CHANGED:

                    break;
            }

        }
    }

    private class ScriptsObserver extends FileObserver {

        public ScriptsObserver(final String path) {
            super(path,
                    FileObserver.CREATE
                            | FileObserver.DELETE
                            | FileObserver.MODIFY
                            | FileObserver.MOVED_FROM
                            | FileObserver.MOVED_TO
            );
        }

        @Override
        public void onEvent(final int event, final String path) {
            if (path == null)
                return;

            Log.d(TAG, "path %s", path);

            final File file = new File(SCRIPTS_DIR, path);
            if (!(file.isDirectory() && new File(file, "main.js").exists()))
                return;

            Log.d(TAG, "file changed %s", file);

            switch (event) {

                case FileObserver.CREATE:
                case FileObserver.MODIFY:
                case FileObserver.MOVED_TO:
                    break;

                case FileObserver.DELETE:
                case FileObserver.MOVED_FROM:
                    break;

            }

//            if (path != null && path.endsWith(".js")) {
//                Log.d(TAG, "file changed: %s", path);
//                mHandler.sendMessage(
//                        mHandler.obtainMessage(COMMAND_SCRIPT_CHANGED, event, 0, path)
//                );
//            }
        }
    }

}
