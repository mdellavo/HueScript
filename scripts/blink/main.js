define(['log', 'hue'], function (Log, Hue) {

    var TAG = __file__.getName();
    var USERNAME = 'huescript-app';
    var DEVICE_TYPE = 'huescript-app';

    var Session = Hue.Session;

    var post = function (f) {
        Handler.post(new java.lang.Runnable({
            run: f
        }));
    };

    var postDelayed = function (f, delay) {
        Handler.postDelayed(new java.lang.Runnable({
            run: f
        }), delay);
    };

    var repeat = function (callback, delay) {


        function timeout() {
            Log.d(TAG, "timer fires!");

            var delay = callback();
            if (delay > 0) {
                Log.d(TAG, "timer firing in %s", delay);
                postDelayed(timeout, delay);
            }
        }

        postDelayed(timeout, delay);
    };

    var repeatFor = function (func, duration, delay) {
        var start = now();
        var count = 0;

        function repeater() {
            var n = now();
            var lapsed = n - start;

            if (lapsed > duration)
                return -1;

            var delay = func(count, start, n);

            count += 1;

            return Math.max(duration - lapsed, delay);
        }

        repeat(repeater, delay);
    };


    var now = function () {
        return java.lang.System.currentTimeMillis();
    };

    var random = new java.util.Random();

    var blinker = function (session, id, hue, duration, delay) {
        var start = now();

        function blink() {
            Log.d(TAG, "blinking %s hue=%s", id, hue);


            session.setLightState(id, {
                'on': true,
                'bri': 100,
                'hue': hue,
                'transitiontime': 1,
                'effect': 'colorloop'
            });

            postDelayed(function () {
                off(session, id)
            }, delay / 2);

            return delay;
        }

        return blink;
    };

    var toggle = function (session, lightId, state) {
        session.setLightState(lightId, {
            'on': state,
            'effect': 'none',
            'bri': 100,
            'sat': 255,
            'transitiontime': 1
        });
    };

    var off = function (session, lightId) {
        toggle(session, lightId, false);
    };

    var on = function (session, lightId) {
        toggle(session, lightId, true);
    };

    var allOff = function (session) {
        session.forEachLight(function (lightId) {
            off(session, lightId);
        });
    };

    var main = function (context) {

        var bridge = "192.168.1.3";

        Session.connect(context, bridge, USERNAME, DEVICE_TYPE, function (session) {
            Log.d(TAG, "starting blink...");

            session.setTrace(true);

            Log.d(TAG, "off!!!");

            allOff(session);

            var hue_max = 65535;
            var delay = 2 * 1000;
            var duration = 30 * 1000;

            var hue = random.nextInt(hue_max)

            function startBlinking(lights) {
                for (var id in lights) {
                    hue = (hue + random.nextInt(hue_max / 10)) % hue_max;
                    var blink = blinker(session, id, hue, duration, delay);
                    repeatFor(blink, duration, delay)
                    blink();
                }
            }

            session.getLights(startBlinking);
        })
    };

    return {
        name: 'Blink',
        color: '#000088',
        description: 'Flash lights one at a time',
        icon: 'http://example.com/icon.png',
        main: main
    };
});



