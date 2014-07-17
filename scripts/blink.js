var USERNAME= 'huescript-app';
var DEVICE_TYPE = 'huescript-app';

var Log = require("log").Log;
var Util = require("util").Util;
var Session = require("hue").Session;

var TAG = __file__.getName();

exports.name = 'Blink';
exports.description = 'Flash lights one at a time';
exports.icon = 'http://example.com/icon.png'

function post(f) {
  Handler.post(new java.lang.Runnable({
    run: f
  }));
}

function postDelayed(f, delay) {
  Handler.postDelayed(new java.lang.Runnable({
    run: f
  }), delay);
}


function repeat(callback, delay) {


  function timeout() {
      Log.d(TAG, "timer fires!");

      var delay = callback();
      if (delay > 0) {
        Log.d(TAG, "timer firing in %s", delay);
        postDelayed(timeout, delay);
      }
  }

  postDelayed(timeout, delay);
}

function repeatFor(func, duration, delay) {
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
}


function now() {
  return java.lang.System.currentTimeMillis();
}

var random = new java.util.Random();

function blinker(session, id, hue, duration, delay) {
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

    postDelayed(function() { off(session, id) }, delay / 2);

    return delay;
  }

  return blink;
}

exports.main = function(context) {

  Session.autoconnect(context, USERNAME, DEVICE_TYPE, function (session) {
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
        hue = (hue + random.nextInt(hue_max/10)) % hue_max;
        var blink = blinker(session, id, hue, duration, delay);
        repeatFor(blink, duration, delay)
        blink();
      }
    }

    session.getLights(startBlinking);
  })
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
