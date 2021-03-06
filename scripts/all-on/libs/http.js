define(["log"], function (Log) {

    var TAG = "net.js";
    var JSON_CONTENT_TYPE = 'application/json';
    var queue = null;

    var Http = {

        getQueue: function (context) {
            if (!queue)
                queue = org.quuux.huescript.HueScriptVolley.getRequestQueue(context);
            return queue;
        },

        request: function (method, url, contentType, data, listener, errorListener) {

            var methodNames = {
                0: "GET",
                1: "POST",
                2: "PUT",
                3: "DELETE"
            };

            var t1 = java.lang.System.currentTimeMillis();

            function complete() {
                var t2 = java.lang.System.currentTimeMillis();
                Log.d(TAG, "%s %s took %sms", methodNames[method], url, t2 - t1);
            }

            var listenerImpl = new com.android.volley.Response.Listener({
                onResponse: function (s) {
                    complete();

                    if (listener) {
                        Handler.post(new java.lang.Runnable({
                            run: function () {
                                listener(s);
                            }
                        }));
                    }
                }
            });

            var errorImpl = new com.android.volley.Response.ErrorListener({
                onErrorResponse: function (e) {
                    Log.e(TAG, "error requesting %s", e, url);
                    complete();

                    if (errorListener) {
                        Handler.post(new java.lang.Runnable({
                            run: function () {
                                errorListener(e);
                            }
                        }));
                    }
                }
            });

            return new org.quuux.huescript.StringRequest(method, url, contentType, data, listenerImpl, errorImpl);
        },

        send: function (context, request) {
            return Http.getQueue(context).add(request);
        },

        get: function (context, url, listener, errorListener) {
            return Http.send(context, Http.request(com.android.volley.Request.Method.GET, url, null, null, listener, errorListener));
        },

        put: function (context, url, contentType, data, listener, errorListener) {
            return Http.send(context, Http.request(com.android.volley.Request.Method.PUT, url, contentType, data, listener, errorListener));
        },
        post: function (context, url, contentType, data, listener, errorListener) {
            return Http.send(context, Http.request(com.android.volley.Request.Method.POST, url, contentType, data, listener, errorListener));
        },
        delete_: function (context, url, contentType, data, listener, errorListener) {
            return Http.send(context, Http.request(com.android.volley.Request.Method.DELETE, url, contentType, data, listener, errorListener));
        },

        jsonParser: function (callback) {
            return function (s) {
                if (callback) {
                    var obj = JSON.parse(s);
                    return callback(obj);
                }

                return undefined;
            };
        },

        getJson: function (context, url, listener, errorListener) {
            return Http.get(context, url, Http.jsonParser(listener), errorListener);
        },
        postJson: function (context, url, data, listener, errorListener) {
            return Http.post(context, url, JSON_CONTENT_TYPE, JSON.stringify(data), Http.jsonParser(listener), errorListener);
        },
        putJson: function (context, url, data, listener, errorListener) {
            return Http.put(context, url, JSON_CONTENT_TYPE, JSON.stringify(data), Http.jsonParser(listener), errorListener);
        },
        deleteJson: function (context, url, data, listener, errorListener) {
            return Http.delete(context, url, JSON_CONTENT_TYPE, JSON.stringify(data), Http.jsonParser(listener), errorListener);
        }

    };

    return Http;
});