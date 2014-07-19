var TAG = "util.js";

var Log = require("./log").Log;

exports.Util = {
  timeit: function(f, msg) {
    var t1 = java.lang.System.currentTimeMillis();
    f();
    var t2 = java.lang.System.currentTimeMillis();
    Log.d(TAG, msg, t2-t1);
  },
  isArray: function(o) {
    return Object.prototype.toString.call(o) === '[object Array]';
  },
  alert: function (context, message) {
    android.widget.Toast.makeText(context, String(message), android.widget.Toast.LENGTH_LONG).show();
  },
  dump: function(tag, msg, o) {
    Log.d(tag, "%s -> %s", msg, JSON.stringify(o, null, 2));
  },
  replace: function(fmt) {
    var params = arguments;

    function replacer(str, position, offset, s) {
      return String(params[Number(position)+1] || '');
    }

    return fmt.replace(/{(\d+)}/g, replacer);
  },
  sleep: function(millis) {
    java.lang.Thread.sleep(millis);
  },
  toast: function(context, text, length) {
    var rv = android.widget.Toast.makeText(context, text, length);
    rv.show();
    return rv;
  }
};