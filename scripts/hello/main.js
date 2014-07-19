var Log = require("log").Log;

var TAG = __file__.getName();

Log.d(TAG, "initializing hello world");

exports.name = 'Hello';
exports.description = 'Hello World';
exports.icon = 'http://example.com/icon.png'

exports.main = function(context) {
  Log.d(TAG, "hello world");
  android.widget.Toast.makeText(context, "Hello World", android.widget.Toast.LENGTH_LONG).show();
}