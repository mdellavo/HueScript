define(["log", "hue"], function(Log, Hue) {
    var TAG = __file__.getName();

    var main = function(context) {
        Log.d(TAG, "all off!!!");

        bridge = "192.168.1.3";

        Session.connect(context, bridge, USERNAME, DEVICE_TYPE, function (session) {
            session.forEachLight(function (lightId) {
                Log.d(TAG, "turning off light: %s", lightId);
                session.setLightState(lightId, {
                    'on': false,
                    'effect': 'none',
                    'bri': 100,
                    'sat': 255,
                    'transitiontime': 1
                });
            });
        });

    };

    return {
        name: 'All Off',
        color: '#ff888888',
        description:'Turn all lights off',
        icon: 'http://quuux.org/color-loop.png',
        main: main
    };
});