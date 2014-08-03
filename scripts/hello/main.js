define(['log'], function(Log) {
    var TAG = __file__.getName();

    Log.d(TAG, "initializing hello world");

    function post(f) {
        Handler.post(new java.lang.Runnable({
            run: f
        }));
    }

    var main = function(context) {

        Log.d(TAG, "hello world");

        post(function() {
            android.widget.Toast.makeText(context, "Hello World", android.widget.Toast.LENGTH_LONG).show();
        });

        java.lang.Thread.sleep(5 * 1000);

        post(function() {
            android.widget.Toast.makeText(context, "Goodbye!", android.widget.Toast.LENGTH_LONG).show();
        });
    };

    return {
        name: 'Hello',
        description: 'Hello World',
        color: '#800',
        background: 'http://example.com/icon.png',
        main: main
    };

});
