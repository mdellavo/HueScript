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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class Sandbox  {

    private static final String TAG = Log.buildTag(Sandbox.class);
    private final InputStreamReader mSrc;
    private final String mName;

    private Scriptable mScope;
    private Context mContext;
    private Function mFunction;

    public Sandbox(final String name, final InputStreamReader src) {
        mName = name;
        mSrc = src;

        init();
    }

    private void init() {
        mContext = Context.enter();
        mScope = mContext.initStandardObjects();

        // NB special android magic
        mContext.setOptimizationLevel(-1);

        // FIXME put these into a "http" namespace
        mScope.put("GET",  mScope, HttpOp.Get());
        mScope.put("PUT",  mScope, HttpOp.Put());
        mScope.put("POST", mScope, HttpOp.Post());

        final WrapFactory wrapFactory = mContext.getWrapFactory();
        define("Log", wrapFactory.wrapJavaClass(mContext, mScope, Log.class));
    }

    public String getName() {
        return mName;
    }

    public boolean run(final android.content.Context context) {
        try {
            mFunction.call(mContext, mScope, mScope, new Object[] {context});
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

    public static Sandbox fromAssetManager(final android.content.Context context, final String filename) {
        final AssetManager assetManager = context.getResources().getAssets();

        final Sandbox rv;
        try {
            final InputStreamReader in = new InputStreamReader(assetManager.open(filename));
            rv = new Sandbox(filename, in);
        } catch (IOException e) {
            Log.e(TAG, "error creating sandbox", e);
            return null;
        }

        rv.define("context", context);

        return rv;
    }

    public static Sandbox fromFile(final android.content.Context context, final File file) {
        final Sandbox rv;
        try {
            final FileInputStream in = new FileInputStream(file);
            rv = new Sandbox(file.getName(), new InputStreamReader(in));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "error loading sandbox from file", e);
            return null;
        }

        rv.define("context", context);

        return rv;
    }

    public boolean load() {

        final Object o;
        try {
            Log.d(TAG, "start execution...");
            o = mContext.evaluateReader(mScope, mSrc, mName, 0, null);
            Log.d(TAG, "execution complete");
        } catch (IOException e) {
            Log.e(TAG, "Error loading script", e);
            return false;
        }

        Log.d(TAG, "script loaded -> %s", o);
        if (!(o instanceof Function)) {
            Log.e(TAG, "script loading did not result in a function!");
            return false;
        }

        mFunction = (Function)o;

        return true;
    }

    public void release() {
        mContext.exit();
    }

    private static class HttpOp extends BaseFunction {

        final int mMethod;

        HttpOp(final int method) {
            mMethod = method;
        }

        @Override
        public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {

            if (!(args[0] instanceof Wrapper)) {
                throw Context.reportRuntimeError("Expected Context as first argument");
            }

            final Object o = ((Wrapper)args[0]).unwrap();
            if (!(o instanceof android.content.Context)) {
                throw Context.reportRuntimeError("Expected Context as first argument");
            }
            final android.content.Context context = (android.content.Context)o;

            if (!(args[1] instanceof CharSequence)) {
                throw Context.reportRuntimeError("Expected String as second argument");
            }

            final String url = args[1].toString();

            int index;
            final String data;
            if (mMethod == Request.Method.GET) {
                data = null;
                index = 2;
            } else {
                if (!(args[2] instanceof CharSequence)) {
                    throw Context.reportRuntimeError("Expected String as third argument");
                }
                data = args[2].toString();
                index = 3;
            }

            final Function callback;
            if (args.length > index) {
                if (!(args[index] instanceof Function) || args[index] == Context.getUndefinedValue()) {
                    throw Context.reportRuntimeError("Expected callback Function as argument");
                }

                callback = (Function)args[index];
                index++;
            } else {
                callback = null;
            }

            final Function errorCallback;
            if (args.length > index) {
                if (!(args[index] instanceof Function) || args[index] == Context.getUndefinedValue()) {
                    throw Context.reportRuntimeError("Expected error callback Function as argument");
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
