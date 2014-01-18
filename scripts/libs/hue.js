
var Log = require("./log").Log;
var Util = require("./util").Util;
var Http = require("./http").Http;

var TAG = 'hue.js';

var Hue = {
  PORTAL_URL: "https://www.meethue.com/api/nupnp",

  ENDPOINT_URL: "http://{0}/{1}",

  endpoint: function(bridge, path) {
    return Util.replace(Hue.ENDPOINT_URL, bridge.internalipaddress, path);
  },

  authEndpoint: function(bridge, username, path) {
    return Hue.endpoint(bridge, "api/" + username + "/" + path);
  },

  discoverBridges: function(context, callback) {
    Http.getJson(context, Hue.PORTAL_URL, callback);
  },

  createUser: function(context, bridge, deviceType, username, callback, errorCallback) {
    var data = {'devicetype': deviceType};

    if (username)
      data['username'] = username;

    Http.postJson(context, Hue.endpoint(bridge, "api"), data, callback, errorCallback);
  },

  getLights: function(context, bridge, username, callback, errorCallback) {
    Http.getJson(context, Hue.authEndpoint(bridge, username, "lights"), callback, errorCallback);
  },

  setLightState: function(context, bridge, username, lightId, state, callback, errorCallback) {
    Http.putJson(context, Hue.authEndpoint(bridge, username, "lights/" + lightId + "/state"), state, callback, errorCallback);
  }

};
exports.Hue = Hue;

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

Session.connect = function(context, bridge, username, deviceType, callback, errorCallback) {

    function connected() {
      callback(new Session(bridge, username));
    }

    function createUser() {
        Hue.createUser(context, bridge, deviceType, username, checkUser, checkUserError);
    }

    function checkUser(o) {
      if (Util.isArray(o) && o[0].error && o[0].error.type == 101) {
        alert(context, "Please press link button!!!");
        Util.sleep(5 * 1000)
        createUser();
      } else {
        connected();
      }
    }

    function checkUserError(e) {
      Log.e(TAG, "check user error", e);
    }

    function checkAuth(o) {
      if (Util.isArray(o) && o[0].error && o[0].error.type == 1) {
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

Session.autoconnect = function(context, username, deviceType, main) {
  function error(e) {
    Log.e(TAG, "error", e);
  }

  Log.d(TAG, "discovering bridges...");

  Hue.discoverBridges(context, function(bridges) {
    for (var i=0; i<bridges.length; i++) {
      Session.connect(context, bridges[i], username, deviceType, main, error);
    }
  });
}

exports.Session = Session;