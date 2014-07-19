
var Log = require("./log").Log;
var Util = require("./util").Util;
var Http = require("./http").Http;

var TAG = 'hue.js';

var Hue = {
  PORTAL_URL: "https://www.meethue.com/api/nupnp",

  ENDPOINT_URL: "http://{0}/{1}",

  endpoint: function(bridge, path) {
    return Util.replace(Hue.ENDPOINT_URL, bridge, path);
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

function Session(context, bridge, username, deviceType) {
  this.context = context;
  this.bridge = bridge;
  this.username = username;
  this.deviceType = deviceType;
  this.lights = null;
  this.trace = false;
  this.execute = true;
}

Session.prototype['setTrace'] = function(state) {
  this.trace = state;
}

Session.prototype['setExecute'] = function(state) {
  this.execute = state;
}

Session.prototype['getLights'] = function(callback, errorCallback) {
  Hue.getLights(this.context, this.bridge, this.username, callback, errorCallback);
};

Session.prototype['forEachLight'] = function(callback) {
  this.getLights(function(lights) {
    for (var id in lights)
      callback(id);
  });
};

Session.prototype['setLightState'] = function(lightId, state, callback, errorCallback) {
  if (this.trace)
    Util.dump(TAG, "setting light " + lightId + " state", state);

  if (this.execute)
    Hue.setLightState(this.context, this.bridge, this.username, lightId, state, callback, errorCallback);
}

Session.connect = function(context, bridge, username, deviceType, callback, errorCallback) {

    function connected() {
      callback(new Session(context, bridge, username, deviceType));
    }

    function createUser() {
        Hue.createUser(context, bridge, deviceType, username, checkUser, checkUserError);
    }

    function checkUser(o) {
      if (Util.isArray(o) && o[0].error && o[0].error.type == 101) {
        Util.alert(context, "Please press link button!!!");
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
      Log.d(TAG, "registering with bridge: %s", bridge);
    }

    Hue.getLights(context, bridge, username, checkAuth, checkAuthError);
}

Session.autoconnect = function(context, username, deviceType, main) {
  function error(e) {
    Log.e(TAG, "error", e);
  }

  Log.d(TAG, "discovering bridges...");

  Hue.discoverBridges(context, function(bridges) {
    Log.d(TAG, "bridges: " + bridges);
    for (var i=0; i<bridges.length; i++) {
      Log.d("discovered bridge: " + bridges[i]);
      Session.connect(context, bridges[i].internalipaddress, username, deviceType, main, error);
    }
  });
}

exports.Session = Session;