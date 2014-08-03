define(["log", "hue"], function(Log, Hue) {
    var TAG = __file__.getName();

    var USERNAME = 'huescript-app';
    var DEVICE_TYPE = 'huescript-app';

    var main = function(context) {
        Log.d(TAG, "all off!!!");

        bridge = "192.168.1.3";

        Hue.Session.connect(context, bridge, USERNAME, DEVICE_TYPE, function (session) {
            session.forEachLight(function (lightId) {
                Log.d(TAG, "turning off light: %s", lightId);
                session.setLightState(lightId, {
                    on: true,
                    effect: 'none',
                    colormode: 'ct',
                    ct: 500,
                    bri: 255,
                    sat: 255,
                    transitiontime: 50
                });
            });
        });

    };

    return {
        name: 'All On',
        color: '#ffcccccc',
        description:'Turn all lights on',
        icon: 'http://quuux.org/color-loop.png',
        main: main
    };
});