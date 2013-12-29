function alert(message) {
    android.widget.Toast.makeText(context, String(message), android.widget.Toast.LENGTH_SHORT).show();
}

function getJson(context, url, callback) {
    GET(context, url, function(s) {
        callback(JSON.parse(s));
    });
}

function postJson(context, url, data, callback) {
    POST(context, url, JSON.stringify(data), function(s) {
        callback(JSON.parse(s));
    });
}

var Hue = {
    PORTAL_URL: "https://www.meethue.com/api/nupnp",

    discoverBridges: function(context, callback) {
        getJson(context, Hue.PORTAL_URL, callback);
    },

    register: function(context, bridge, callback) {
        postJSON()
    }
};

(function() {

    var TAG = 'SANDBOX(test.js)';

    Log.d(TAG, "discovering bridges...");

    Hue.discoverBridges(context, function(bridges) {
        for (var i=0; i<bridges.length; i++) {
            Log.d(TAG, "bridge: %s", bridges[i].internalipaddress);
        }
    });




})();
