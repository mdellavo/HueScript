package org.quuux.huescript;

import android.content.res.AssetManager;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.Wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Sandbox  {

    private static final String TAG = Log.buildTag(Sandbox.class);
    private final String mName;

    private Scriptable mScope;
    private NativeObject mScript;

    public Sandbox(final String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public String getScriptName() {
        return (String) mScript.get("name");
    }

    public String getScriptDescription() {
        return (String) mScript.get("description");
    }

    public boolean run(final android.content.Context context) {
        try {
            final Function func = (Function) mScript.get("main");

            ContextFactory.getGlobal().call(new ContextAction() {
                @Override
                public Object run(final Context context) {
                    init(context);
                    func.call(context, mScope, mScope, new Object[]{context});
                    return null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "error running script", e);
            return false;
        }

        return true;
    }

    public void define(final String name, final Object value) {
        ContextFactory.getGlobal().call(new ContextAction() {
            @Override
            public Object run(final Context context) {
                init(context);
                define(context, name, value);
                return null;
            }
        });
    }

    private void init(final Context context) {
        if (mScope == null)
            mScope = context.initStandardObjects();

        // NB special android magic
        context.setOptimizationLevel(-1);

        final WrapFactory wrapFactory = context.getWrapFactory();
        wrapFactory.setJavaPrimitiveWrap(false);
        define(context, "Log", wrapFactory.wrapJavaClass(context, mScope, Log.class));
    }

    private void define(final Context context, final String name, final Object value) {
        final WrapFactory wrapFactory = context.getWrapFactory();
        final Object wrapped = wrapFactory.wrap(context, mScope, value, null);
        mScope.put(name, mScope, wrapped);
    }

    public void evaluate(final File src) {

        ContextFactory.getGlobal().call(new ContextAction() {
            @Override
            public Object run(final Context context) {

                init(context);

                define(context, "load", new LoadScript(src));

                final Object o;
                try {
                    Log.d(TAG, "start execution...");
                    o = context.evaluateReader(mScope, new BufferedReader(new FileReader(src)), mName, 0, null);
                    Log.d(TAG, "execution complete");
                } catch (IOException e) {
                    Log.e(TAG, "Error loading script", e);
                    return false;
                }

                Log.d(TAG, "script loaded: %s -> %s", o, context.toString(o));
                if (!(o instanceof Function)) {
                    Log.e(TAG, "script loading did not result in a function!");
                    return false;
                }

                final Function func = (Function) o;
                mScript = (NativeObject) func.call(context, mScope, mScope, new Object[]{});

                return null;
            }
        });
    }

    public class LoadScript extends BaseFunction {

        private final File mFile;

        public LoadScript(final File f) {
            mFile = f;
        }

        @Override
        public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {

            if (args.length < 1) {
                throw Context.reportRuntimeError("Expected String as first argument");
            }

            if (!(args[0] instanceof CharSequence)) {
                throw Context.reportRuntimeError(String.format("Expected String as first argument, got %s", args[0].getClass()));
            }

            final String path = (String) args[0];
            final File f = new File(mFile.getParentFile(), path);

            Log.d(TAG, "loading %s", path);

            try {
                cx.evaluateReader(mScope, new BufferedReader(new FileReader(f)), mName, 0, null);
            } catch (IOException e) {
                Log.e(TAG, "Error loading script", e);
            }

            return thisObj;
        }
    }
}
