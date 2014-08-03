package org.quuux.huescript;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sandbox {

    private static final String TAG = Log.buildTag(Sandbox.class);
    public static final String ACTION_CALL_START = "org.quuux.huescript.intents.CALL_START";
    public static final String ACTION_CALL_END = "org.quuux.huescript.intents.CALL_END";

    private final File mPath;
    private final File mModulesPath;
    private Scriptable mScope;
    private Require mRequire;
    private Scriptable mExports;
    private Handler mHandler;
    private ModuleCache mModuleCache = new ModuleCache();

    private Thread mWorkerThread;
    private Runnable mWorker = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler(Looper.myLooper());

            require();

            try {
                Looper.loop();
            } catch(Exception e) {
                Log.e(TAG, "worker error", e);
            }
        }
    };
    private boolean mInitialized;
    private String color;

    public Sandbox(final File path) {
        mPath = path;
        mModulesPath = new File(path, "libs");
        mWorkerThread = new Thread(null, mWorker, String.format("sandbox(%s)", mPath), Integer.MAX_VALUE);
        mWorkerThread.start();

    }

    public Object getExport(final String name) {
        return mExports != null ? (String) mExports.get(name, mExports) : null;
    }

    public File getPath() {
        return mPath;
    }

    public String getName() {
        return mPath.getName();
    }

    public String getScriptName() {
        return (String) getExport("name");
    }

    public String getScriptDescription() {
        return (String) getExport("description");
    }

    public String getColor() {
        return (String) getExport("color");za
    }

    private void postWithContext(final ContextAction runnable) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ContextFactory.getGlobal().call(new ContextAction() {
                    @Override
                    public Object run(final Context context) {
                        if (!mInitialized) {
                            mInitialized = true;
                            init(context);
                        }

                        return runnable.run(context);
                    }
                });
            }
        });
    }


    public void callExport(final android.content.Context androidContext, final String method, final Object... args) {

        postWithContext(new ContextAction() {
            @Override
            public Object run(final Context context) {

                final Function func = (Function) mExports.get(method, mExports);

                broadcastUpdate(androidContext, ACTION_CALL_START, method);

                try {
                    func.call(context, mScope, mScope, args);
                } catch(Exception e) {
                    Log.e(TAG, "error calling export method %s", e, method);
                }

                broadcastUpdate(androidContext, ACTION_CALL_END, method);

                return null;
            }
        });

    }

    private void broadcastUpdate(final android.content.Context context, final String action, final String method) {
        final Intent intent = new Intent(action);
        intent.putExtra("name", getName());
        intent.putExtra("method", method);
        Log.d(TAG, "sending -> %s", intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void init(final Context context) {
        Log.d(TAG, "initing sandbox");

        final WrapFactory wrapFactory = context.getWrapFactory();
        wrapFactory.setJavaPrimitiveWrap(false);

        context.setLanguageVersion(170);
        context.setOptimizationLevel(-1);

        if (mRequire == null) {
            mScope = context.initStandardObjects();

            mScope.put("Handler", mScope, mHandler);
            mScope.put("__file__", mScope, new File(mPath, "main.js"));
            mScope.put("__defined__", mScope, mModuleCache);

            List<String> paths = Arrays.asList(mPath.getAbsolutePath(), mModulesPath.getAbsolutePath());

            List<URI> uris = new ArrayList<URI>();
            for (String path : paths) {
                try {
                    URI uri = new URI(path);
                    if (!uri.isAbsolute()) {
                        // call resolve("") to canonify the path
                        uri = new File(path).toURI().resolve("");
                    }
                    if (!uri.toString().endsWith("/")) {
                        // make sure URI always terminates with slash to
                        // avoid loading from unintended locations
                        uri = new URI(uri + "/");
                    }
                    uris.add(uri);
                } catch (URISyntaxException e) {
                    Log.e(TAG, "error loading module path", e);
                }
            }

            Script define = null;
            try {
                define = context.compileReader(
                        new FileReader(new File(mModulesPath.getAbsolutePath(), "require.js")),
                        "require.js",
                        1,
                        null
                );
            } catch (FileNotFoundException e) {
                Log.e(TAG, "error installing define()", e);
            } catch (IOException e) {
                Log.e(TAG, "error installing define()", e);
            }

            final ModuleScriptProvider provider = new SoftCachingModuleScriptProvider(new UrlModuleSourceProvider(uris, null));
            mRequire = new RequireBuilder()
                    .setModuleScriptProvider(provider)
                    .setPreExec(define)
                    .createRequire(context, mScope);
            mRequire.install(mScope);

        }

    }

    public void require(final String name) {
        Log.d(TAG, "requiring %s", name);

        postWithContext(new ContextAction() {
            @Override
            public Object run(final Context context) {
                try {
                    init(context);
                    Scriptable exports = mRequire.requireMain(context, name);
                    Log.d(TAG, "exports = %s", exports);
                    mExports = (Scriptable) exports.get("defined", exports);
                } catch (EcmaError e) {
                    Log.d(TAG, "could not load %s", e, name);
                }
                return null;
            }
        });
    }

    public void require() {
        require("main");
    }


    class ModuleCache extends ScriptableObject {

        @Override
        public String getClassName() {
            return "ModuleCache";
        }

        @Override
        public Object getDefaultValue(final Class<?> typeHint) {
            return toString();
        }
    }

}
