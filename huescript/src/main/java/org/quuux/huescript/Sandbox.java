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

import java.io.IOException;
import java.io.InputStreamReader;

public class Sandbox implements Runnable {

    private static final String TAG = Log.buildTag(Sandbox.class);
    private final InputStreamReader mSrc;
    private final String mName;
    private NativeObject mScope;
    private ScriptableObject mGlobal;

    public Sandbox(final String name, final InputStreamReader src) {
        mName = name;
        mSrc = src;

        init();
    }

    private void init() {
        ContextFactory.getGlobal().call(new ContextAction() {
            @Override
            public Object run(final Context context) {

                mGlobal = context.initStandardObjects();
                mScope = new NativeObject();
                mScope.setPrototype(mGlobal);

                final Scriptable packages = (Scriptable) mGlobal.get("Packages", mGlobal);
                final Object android = packages.get("android", packages);
                mGlobal.defineProperty("android", android, ScriptableObject.DONTENUM);

                // FIXME put these into a "http" namespace
                mGlobal.defineProperty("GET", HttpOp.Get(), ScriptableObject.DONTENUM);
                mGlobal.defineProperty("PUT", HttpOp.Get(), ScriptableObject.DONTENUM);
                mGlobal.defineProperty("POST", HttpOp.Get(), ScriptableObject.DONTENUM);

                final WrapFactory wrapFactory = context.getWrapFactory();
                define("Log", wrapFactory.wrapJavaClass(context, mGlobal, Log.class));

                return null;
            }
        });
    }

    @Override
    public void run() {
        evaluate();
    }

    public void define(final String name, final Object value) {
        ContextFactory.getGlobal().call(new ContextAction() {
            public Object run(org.mozilla.javascript.Context cx) {
                final WrapFactory wrapFactory = cx.getWrapFactory();
                final Object wrapped = wrapFactory.wrap(cx, mGlobal, value, null);
                mScope.put(name, mScope, wrapped);
                return null;
            }
        });
    }

    public void evaluate() {
        ContextFactory.getGlobal().call(new ContextAction() {
            @Override
            public Object run(final Context context) {

                // NB special android magic
                context.setOptimizationLevel(-1);

                try {
                    context.evaluateReader(mScope, mSrc, mName, 0, null);
                } catch (IOException e) {
                    Log.e(TAG, "error evaluating script", e);
                }

                return null;
            }
        });
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

            final String data;
            if (mMethod == Request.Method.GET) {
                data = null;
            } else {
                if (!(args[2] instanceof CharSequence)) {
                    throw Context.reportRuntimeError("Expected String as third argument");
                }
                data = args[2].toString();
            }

            final int offset = data == null ? 0 : 1;

            if (!(args[2 + offset] instanceof Function)) {
                throw Context.reportRuntimeError("Expected callback Function as argument");
            }

            final Function callback = (Function)args[2 + offset];

            final Function errorCallback;
            if (args.length > (2 + offset)) {

                if (!(args[3 + offset] instanceof Function)) {
                    throw Context.reportRuntimeError("Expected error callback Function as argument");
                }

                errorCallback = (Function)args[3 + offset];

            } else {
                errorCallback = null;
            }

            final Response.Listener<String> listener = new Response.Listener<String>() {
                @Override
                public void onResponse(final String s) {

                    Log.d(TAG, "response: %s", s);

                    ContextFactory.getGlobal().call(new ContextAction() {
                        @Override
                        public Object run(final Context context) {
                            callback.call(cx, scope, thisObj, new Object[]{s});
                            return null;
                        }
                    });
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
                                errorCallback.call(cx, scope, thisObj, new Object[]{volleyError});
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
