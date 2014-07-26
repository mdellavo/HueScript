var USERNAME= 'huescript-app';
var DEVICE_TYPE = 'huescript-app';

var Session = require("hue").Session;
var Log = require("log").Log;
var Util = require("util").Util;

var TAG = __file__.getName();

exports.name = 'Color Loop';
exports.description = 'A wobbly loop through colors';
exports.icon = 'http://quuux.org/color-loop.png'

exports.main = function(context) {
  bridge = "192.168.1.3";
  Session.connect(context, bridge, USERNAME, DEVICE_TYPE, function (session) {

    allOff(session);
    Util.sleep(5000);

    var random = new java.util.Random();

    var hue_max = 65535;
    var hue_slop = 0;

    var hue = random.nextInt(hue_max);

    session.forEachLight(function(lightId) {
      hue_slop += random.nextInt(hue_max / 10);

      session.setLightState(lightId, {
        'on': true,
        'bri': random.nextInt(100),
        'hue': Math.round((hue + hue_slop) % hue_max),
        'sat': 255,
        'effect': 'colorloop',
        'transitiontime': random.nextInt(100)
      }, function(json) {
           Util.dump(TAG, "set light state json", json);
         });

      Util.sleep(random.nextInt(3000));

    });
  });
};


function toggle(session, lightId, state) {
  session.setLightState(lightId, {
    'on': state,
    'effect': 'none',
    'bri': 100,
    'sat': 255,
    'transitiontime': 1
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
