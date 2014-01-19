var USERNAME= 'huescript-app';
var DEVICE_TYPE = 'huescript-app';

var Session = require("hue").Session;

exports.name = 'Color Loop';
exports.description = 'A wobbly loop through colors';
exports.icon = 'http://quuux.org/color-loop.png'

exports.main = function(context) {

  Session.autoconnect(context, USERNAME, DEVICE_TYPE, function (session) {

    var random = new java.util.Random();

    var hue_max = 65535;
    var hue = random.nextInt(hue_max);
    var hue_slop = 0;

    session.forEachLight(function(lightId) {
      hue_slop += random.nextInt(hue_max/10)

      session.setLightState(lightId, {
        'on': true,
        'bri': 100,
        'hue': (hue + hue_slop) % hue_max,
        'sat': 255,
        'effect': 'colorloop',
        'transitiontime': 10
      });

    });
  });
};
