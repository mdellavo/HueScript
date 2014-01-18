var TAG = "net.js";

var Log = require("./log").Log;
var Util = require("./util").Util;

exports.Http = {
  JSON_CONTENT_TYPE: 'application/json',

  getQueue: function(context) {
    var rv = org.quuux.huescript.HueScriptVolley.getRequestQueue;
    return rv(context);
  },

  request: function(method, url, contentType, data, listener, errorListener) {

    var methodNames = {
      0: "GET",
      1: "POST",
      2: "PUT",
      3: "DELETE"
    };

    var t1 = java.lang.System.currentTimeMillis();
    function complete() {
      var t2 = java.lang.System.currentTimeMillis();
      Log.d(TAG, "%s %s took %sms", methodNames[method], url, t2-t1);
    }

    var listenerImpl = new com.android.volley.Response.Listener({
      onResponse: function(s) {
        complete();

        if (listener)
          listener(s);
      }
    });

    var errorImpl = new com.android.volley.Response.ErrorListener({
      onErrorResponse: function(e) {
        Log.e("error requesting %s", e, url);
        complete();

        if (errorListener)
          errorListener(e);
      }
    });

    return new org.quuux.huescript.StringRequest(method, url, contentType, data, listenerImpl, errorImpl);
  },

  send: function(context, request) {
    return Http.getQueue(context).add(request);
  },

  get: function(context, url, listener, errorListener) {
    return Http.send(context, Http.request(com.android.volley.Request.Method.GET, url, null, null, listener, errorListener));
  },

  put: function(context, url, contentType, data, listener, errorListener) {
    return Http.send(context, Http.request(com.android.volley.Request.Method.PUT, url, contentType, data, listener, errorListener));
  },
  post: function(context, url, contentType, data, listener, errorListener) {
    return Http.send(context, Http.request(com.android.volley.Request.Method.POST, url, contentType, data, listener, errorListener));
  },
  delete_: function(context, url, contentType, data, listener, errorListener) {
    return Http.send(context, Http.request(com.android.volley.Request.Method.DELETE, url, contentType, data, listener, errorListener));
  },

  jsonParser: function(callback) {
    return function(s) {
      if (callback) {
        var obj = JSON.parse(s);
        Util.dump(TAG, 'json', obj);
        return callback(obj);
      }

      return undefined;
    };
  },

  getJson: function(context, url, listener, errorListener){
    return Http.get(context, url, Http.jsonParser(listener), errorListener);
  },
  postJson: function(context, url, data, listener, errorListener){
    return Http.post(context, url, Http.JSON_CONTENT_TYPE, JSON.stringify(data), Http.jsonParser(listener), errorListener);
  },
  putJson: function(context, url, data, listener, errorListener){
    return Http.put(context, url, Http.JSON_CONTENT_TYPE, JSON.stringify(data), Http.jsonParser(listener), errorListener);
  },
  deleteJson: function(context, url, data, listener, errorListener){
    return Http.delete(context, url, Http.JSON_CONTENT_TYPE, JSON.stringify(data), Http.jsonParser(listener), errorListener);
  }

};
