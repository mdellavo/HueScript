package org.quuux.huescript;

import android.os.Handler;
import android.os.Looper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.tools.shell.Global;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Sandbox {

    private static final String TAG = Log.buildTag(Sandbox.class);

    private final File mPath;
    private final File mModulesPath;
    private Global mScope = new Global();
    private Require mRequire;
    private Scriptable mExports;
    private Handler mHandler;

    private Thread mWorkerThread;
    private Runnable mWorker = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler();
            Looper.loop();
        }
    };

    public Sandbox(final File path, final File modulesPath) {
        mPath = path;
        mModulesPath = modulesPath;
        mWorkerThread = new Thread(mWorker);
        mWorkerThread.start();
    }

    public String getName() {
        final String name = mPath.getName();
        return name.substring(0, name.length() - 3);
    }

    public String getScriptName() {
        return (String) mExports.get("name", mExports);
    }

    public String getScriptDescription() {
        return (String) mExports.get("description", mExports);
    }

    private void postWithContext(final ContextAction runnable) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ContextFactory.getGlobal().call(new ContextAction() {
                    @Override
                    public Object run(final Context context) {
                        init(context);
                        return runnable.run(context);
                    }
                });
            }
        });
    }


    public void callExport(final String name, final Object... args) {

        postWithContext(new ContextAction() {
            @Override
            public Object run(final Context context) {
                final Function func = (Function) mExports.get(name, mExports);

                try {
                    func.call(context, mScope, mScope, args);
                } catch(Exception e) {
                    Log.e(TAG, "error calling export function %s", e, name);
                }

                return null;
            }
        });

    }

    private void init(final Context context) {
        Log.d(TAG, "initing sandbox");

        if (mRequire == null) {
            mScope.initStandardObjects(context, false);

            List<String> paths = Arrays.asList(mPath.getParent(), mModulesPath.getAbsolutePath());
            mRequire = mScope.installRequire(context, paths, false);

            mScope.put("Handler", mScope, mHandler);
            mScope.put("__file__", mScope, mPath);
        }

        final WrapFactory wrapFactory = context.getWrapFactory();
        wrapFactory.setJavaPrimitiveWrap(false);

        context.setLanguageVersion(170);
        context.setOptimizationLevel(-1);
    }

    public void require(final String name) {
        Log.d(TAG, "requiring %s", name);

        ContextFactory.getGlobal().call(new ContextAction() {
            @Override
            public Object run(final Context context) {
                init(context);
                mExports = mRequire.requireMain(context, name);
                return null;
            }
        });
    }

    public void require() {
        require(getName());
    }

}
