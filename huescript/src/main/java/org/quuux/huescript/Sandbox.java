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
    private Context mContext;
    private NativeObject mScript;

    public Sandbox(final String name) {
        mName = name;
        init();
    }

    private void init() {
        mContext = Context.enter();
        mScope = mContext.initStandardObjects();

        // NB special android magic
        mContext.setOptimizationLevel(-1);

        // FIXME implement in js
        define("GET", HttpOp.Get());
        define("PUT", HttpOp.Put());
        define("POST", HttpOp.Post());

        final WrapFactory wrapFactory = mContext.getWrapFactory();
        wrapFactory.setJavaPrimitiveWrap(false);
        define("Log", wrapFactory.wrapJavaClass(mContext, mScope, Log.class));
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
            func.call(mContext, mScope, mScope, new Object[] {context});
        } catch (Exception e) {
            Log.e(TAG, "error running script", e);
            return false;
        }

        return true;
    }

    public void define(final String name, final Object value) {
        final WrapFactory wrapFactory = mContext.getWrapFactory();
        final Object wrapped = wrapFactory.wrap(mContext, mScope, value, null);
        mScope.put(name, mScope, wrapped);
    }

    public boolean evaluate(final File src) {

        define("load", new LoadScript(src));

        final Object o;
        try {
            Log.d(TAG, "start execution...");
            o = mContext.evaluateReader(mScope, new BufferedReader(new FileReader(src)), mName, 0, null);
            Log.d(TAG, "execution complete");
        } catch (IOException e) {
            Log.e(TAG, "Error loading script", e);
            return false;
        }

        Log.d(TAG, "script loaded: %s -> %s", o, mContext.toString(o));
        if (!(o instanceof Function)) {
            Log.e(TAG, "script loading did not result in a function!");
            return false;
        }

        final Function func = (Function)o;
        mScript = (NativeObject) func.call(mContext, mScope, mScope, new Object[] {});

        return true;
    }

    public void release() {
        mContext.exit();
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

    private static class HttpOp extends BaseFunction {

        final int mMethod;

        HttpOp(final int method) {
            mMethod = method;
        }

        @Override
        public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {

            if (args.length < 2) {
                throw Context.reportRuntimeError("Incorrect number of parameters");
            }

            if (!(args[0] instanceof Wrapper)) {
                throw Context.reportRuntimeError(
                        String.format("Expected Context as first argument, got %s", args[0].getClass())
                );
            }

            final Object o = ((Wrapper)args[0]).unwrap();
            if (!(o instanceof android.content.Context)) {
                throw Context.reportRuntimeError(
                        String.format("Expected Context as first argument, got %s", o.getClass())
                );
            }

            final android.content.Context context = (android.content.Context)o;

            if (!(args[1] instanceof CharSequence)) {
                throw Context.reportRuntimeError(
                        String.format("Expected String as second argument, got %s", args[1].getClass())
                );
            }

            final String url = args[1].toString();

            int index;
            final String data;
            if (mMethod == Request.Method.GET) {
                data = null;
                index = 2;
            } else {
                if (!(args[2] instanceof CharSequence)) {
                    throw Context.reportRuntimeError(
                            String.format("Expected String as third argument, got %s", args[2].getClass())
                    );
                }
                data = args[2].toString();
                index = 3;
            }

            final Function callback;
            if (args.length > index) {
                if (!(args[index] instanceof Function) || args[index] == Context.getUndefinedValue()) {
                    throw Context.reportRuntimeError(
                            String.format("Expected callback Function as argument, got %s", args[index].getClass())
                    );
                }

                callback = (Function)args[index];
                index++;
            } else {
                callback = null;
            }

            final Function errorCallback;
            if (args.length > index) {
                if (!(args[index] instanceof Function) || args[index] == Context.getUndefinedValue()) {
                    throw Context.reportRuntimeError(
                            String.format("Expected error callback Function as argument, got %s", args[index].getClass())
                    );
                }

                errorCallback = (Function)args[index];

            } else {
                errorCallback = null;
            }

            final Response.Listener<String> listener = new Response.Listener<String>() {
                @Override
                public void onResponse(final String s) {

                    Log.d(TAG, "response: %s", s);

                    if (callback != null) {
                        ContextFactory.getGlobal().call(new ContextAction() {
                            @Override
                            public Object run(final Context context) {
                                try {
                                    callback.call(cx, scope, thisObj, new Object[] { s });
                                } catch (Exception e) {
                                    Log.d(TAG, "error calling callback", e);
                                }
                                return null;
                            }
                        });
                    }
                }
            };

            final Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError volleyError) {

                    Log.e(TAG, "error", volleyError);

                    if (errorCallback != null) {
                        ContextFactory.getGlobal().call(new ContextAction() {
                            @Override
                            public Object run(final Context context) {
                                try {
                                    errorCallback.call(cx, scope, thisObj, new Object[]{volleyError});
                                } catch (Exception e) {
                                    Log.d(TAG, "error calling callback", e);
                                }
                                return null;
                            }
                        });
                    }
                }
            };

            final JsonRequest<String> request = new JsonRequest<String>(mMethod, url, data, listener, errorListener) {
                @Override
                protected Response<String> parseNetworkResponse(final NetworkResponse networkResponse) {
                    return Response.success(
                            new String(networkResponse.data),
                            HttpHeaderParser.parseCacheHeaders(networkResponse)
                    );
                }
            };


            HueScriptVolley.getRequestQueue(context).add(request);

            return thisObj;
        }

        public static HttpOp Get() {
            return new HttpOp(Request.Method.GET);
        }

        public static HttpOp Post() {
            return new HttpOp(Request.Method.POST);
        }

        public static HttpOp Put() {
            return new HttpOp(Request.Method.PUT);
        }

    }
}
