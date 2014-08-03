define(['log'], function(Log) {
    var TAG = __file__.getName();

    Log.d(TAG, "initializing hello world");

    function postDelayed(f, ms) {
        Handler.postDelayed(new java.lang.Runnable({
            run: f
        }), ms);
    }

    var main = function(context) {

        Log.d(TAG, "hello world");

        postDelayed(function() {
            android.widget.Toast.makeText(context, "Hello World", android.widget.Toast.LENGTH_LONG).show();
        }, 1000);

        postDelayed(function() {
            android.widget.Toast.makeText(context, "Goodbye!", android.widget.Toast.LENGTH_LONG).show();
        }, 5000);
    };

    return {
        name: 'Hello',
        description: 'Hello World',
        color: '#ff880000',
        background: 'http://example.com/icon.png',
        main: main
    };

});
