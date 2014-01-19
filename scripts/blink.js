var USERNAME= 'huescript-app';
var DEVICE_TYPE = 'huescript-app';

var Log = require("log").Log;
var Util = require("util").Util;
var Session = require("hue").Session;

var TAG = "test.js";

exports.name = 'Blink';
exports.description = 'Flash lights one at a time';
exports.icon = 'http://example.com/icon.png'

exports.main = function(context) {

  Session.autoconnect(context, USERNAME, DEVICE_TYPE, function (session) {
    session.setTrace(true);

    var random = new java.util.Random();

    var hue_max = 65535;
    var hue = random.nextInt(hue_max);
    var hue_slop = 0;

    Log.d(TAG, "off!!!");
    allOff(session);

    session.getLights(
      function(lights) {
        while (true) {
          for (var id in lights) {
            var light = lights[id];
            var hue = (hue + random.nextInt(hue_max/10)) % hue_max;
            blink(session, id, hue);
          }
        }
      }
    );
  })
};

function toggle(session, lightId, state) {
  session.setLightState(lightId, {
    'on': state,
    'effect': 'none',
    'bri': 100,
    'sat': 255
  });
}

function off(session, lightId) {
  toggle(session, lightId, false);
}

function on(session, lightId) {
  toggle(session, lightId, true);
}

function allOff(session) {
  session.forEachLight(function(lightId) {
    off(session, lightId);
  });
}

function blink(session, lightId, hue) {
  Log.d(TAG, "blinking %s hue=%s", lightId, hue);


  session.setLightState(lightId, {
    'on': true,
    'bri': 100,
    'hue': hue,
    'transitiontime': 1
  });

  Util.sleep(1 * 1000);

  off(session, lightId);
}