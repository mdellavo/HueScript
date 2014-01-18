var Net = {
  JSON_CONTENT_TYPE: 'application/json',

  getQueue: function(context) {
    return org.quuux.hueScript.HueScriptVolley.getRequestQueue(context)
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
      Log.d(TAG, "%s %s took %0.2ms", methodNames[method], url, t2-t1);
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

    // var request = new JavaAdapter(com.android.volley.toolbox.StringRequest, {
    //   getBodyContentType: function() {
    //     return contentType ? contentType : super.getBodyContentType();
    //   },
    //   getBody: function() {
    //     return data ? data : super.getBody();
    //   }
    // });

    return new request(method, url, listenerImpl, errorImpl);
  },

  send: function(context, request) {
    return Net.getQueue(context).add(request);
  },

  get: function(context, url, listener, errorListener) {
    return Net.send(context, Net.request(com.android.volley.Request.Method.GET, url, null, null, listener, errorListener));
  },

  put: function(context, url, contentType, data, listener, errorListener) {
    return Net.send(context, Net.request(com.android.volley.Request.Method.PUT, url, contentType, data, listener, errorListener));
  },
  post: function(context, url, contentType, data, listener, errorListener) {
    return Net.send(context, Net.request(com.android.volley.Request.Method.POST, url, contentType, data, listener, errorListener));
  },
  delete_: function(context, url, contentType, data, listener, errorListener) {
    return Net.send(context, Net.request(com.android.volley.Request.Method.DELETE, url, contentType, data, listener, errorListener));
  },

  jsonParser: function(callback) {
    return function(s) {
      if (callback)
        return callback(JSON.parse(s));

      return undefined;
    };
  },

  getJson: function(context, url, listener, errorListener){
    return Net.get(context, url, Net.JSON_CONTENT_TYPE, Net.jsonParser(listener), errorListener);
  },
  postJson: function(context, url, data, listener, errorListener){
    return Net.get(context, url, Net.JSON_CONTENT_TYPE, Net.jsonParser(listener), errorListener);
  },
  putJson: function(context, url, data, listener, errorListener){
    return Net.get(context, url, Net.JSON_CONTENT_TYPE, Net.jsonParser(listener), errorListener);
  },
  deleteJson: function(context, url, data, listener, errorListener){
    return Net.get(context, url, Net.JSON_CONTENT_TYPE, data, Net.jsonParser(listener), errorListener);
  }

};
