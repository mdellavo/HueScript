var USERNAME= 'huescript-app';
var DEVICE_TYPE = 'huescript-app';

load('libs/hue.js');

function main(context) {

  Session.autoconnect(context, USERNAME, function (session) {

    var random = new java.util.Random();

    var hue_max = 65535;
       var hue = random.nextInt(hue_max);
    var hue_slop = 0;

    session.forEachLight(context, function(lightId) {
      hue_slop += random.nextInt(hue_max/10)

      session.setLightState(context, lightId, {
        'on': true,
        'bri': 50,
        'hue': (hue + hue_slop) % hue_max,
           'sat': 255,
        'transitiontime': 10
      });

    });
  });
}

function() {
  return {
   'name': 'Test Script',
   'description': 'A simple test script',
   'main': main
 };
}
