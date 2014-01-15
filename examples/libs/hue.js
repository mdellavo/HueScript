var TAG = 'hue.js';

function isArray(o) {
  return Object.prototype.toString.call(o) === '[object Array]';
}

function alert(context, message) {
    android.widget.Toast.makeText(context, String(message), android.widget.Toast.LENGTH_LONG).show();
}

function dump(tag, msg, o) {
  Log.d(tag, "%s -> %s", msg, JSON.stringify(o));
}

function replace(fmt) {
    var params = arguments;

    function replacer(str, position, offset, s) {
        return String(params[Number(position)+1] || '');
    }

    return fmt.replace(/{(\d+)}/g, replacer);
}

function getJson(context, url, callback) {
  Log.d(TAG, "GET %s", url);
  GET(context, url, function(s) {
    if (callback)
      callback(JSON.parse(s));
  });
}

function postJson(context, url, params, callback) {
  var data = JSON.stringify(params);
  Log.d(TAG, "POST %s : %s", url, data);
  POST(context, url, data, function(s) {
    if (callback)
      callback(JSON.parse(s));
  });
}

function putJson(context, url, params, callback) {
  var data = JSON.stringify(params);
  Log.d(TAG, "PUT %s : %s", url, data);
  PUT(context, url, data, function(s) {
    if (callback)
      callback(JSON.parse(s));
  });
}

function sleep(millis) {
  java.lang.Thread.sleep(millis);
}

var Hue = {
  PORTAL_URL: "https://www.meethue.com/api/nupnp",

  ENDPOINT_URL: "http://{0}/{1}",

  endpoint: function(bridge, path) {
    return replace(Hue.ENDPOINT_URL, bridge.internalipaddress, path);
  },

  authEndpoint: function(bridge, username, path) {
    return Hue.endpoint(bridge, "api/" + username + "/" + path);
  },

  discoverBridges: function(context, callback) {
    getJson(context, Hue.PORTAL_URL, callback);
  },

  createUser: function(context, bridge, deviceType, username, callback, errorCallback) {
    var data = {'devicetype': deviceType};

    if (username)
      data['username'] = username;

    postJson(context, Hue.endpoint(bridge, "api"), data, callback, errorCallback);
  },

  getLights: function(context, bridge, username, callback, errorCallback) {
    getJson(context, Hue.authEndpoint(bridge, username, "lights"), callback, errorCallback);
  },

  setLightState: function(context, bridge, username, lightId, state, callback, errorCallback) {
    putJson(context, Hue.authEndpoint(bridge, username, "lights/" + lightId + "/state"), state, callback, errorCallback);
  }

};

function Session(bridge, username) {
  this.bridge = bridge;
  this.username = username;
  this.lights = null;
}

Session.prototype['getLights'] = function(context, callback, errorCallback) {
  if (this.lights != null) {
    callback(this.lights);
    return;
  }

  var this_ = this;
  function cachingCallback(lights) {
    this_.lights = lights;
    callback(lights);
  }

  Hue.getLights(context, this.bridge, this.username, cachingCallback, errorCallback);
};

Session.prototype['forEachLight'] = function(context, callback) {
  this.getLights(context, function(lights) {
    for (var id in lights)
      callback(id);
  });
};

Session.prototype['setLightState'] = function(context, lightId, state, callback, errorCallback) {
  Hue.setLightState(context, this.bridge, this.username, lightId, state, callback, errorCallback);
}

function connect(context, bridge, username, callback, errorCallback) {

    function connected() {
      callback(new Session(bridge, username));
    }

    function createUser() {
        Hue.createUser(context, bridge, DEVICE_TYPE, username, checkUser, checkUserError);
    }

    function checkUser(o) {
      if (isArray(o) && o[0].error && o[0].error.type == 101) {
        alert(context, "Please press link button!!!");
        sleep(5 * 1000)
        createUser();
      } else {
        connected();
      }
    }

    function checkUserError(e) {
      Log.e(TAG, "check user error", e);
    }

    function checkAuth(o) {
      if (isArray(o) && o[0].error && o[0].error.type == 1) {
        createUser();
      } else {
        connected();
      }
    }

    function checkAuthError(e) {
      Log.d(TAG, "registering with bridge: %s", bridge.internalipaddress);
    }

    Hue.getLights(context, bridge, username, checkAuth, checkAuthError);
}

function autoconnect(context, username, main) {
  function error(e) {
    Log.e(TAG, "error", e);
  }

  Log.d(TAG, "discovering bridges...");
  Hue.discoverBridges(context, function(bridges) {
    for (var i=0; i<bridges.length; i++) {
      connect(context, bridges[i], username, main, error);
    }
  });
}