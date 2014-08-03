
function define() {

    var TAG = __file__.getName();

    var id, dependencies, factory;

    if (arguments.length == 3) {
        id = arguments[0];
        dependencies = arguments[1];
        factory = arguments[2];
    } else if (arguments.length == 2) {
        id = null;
        dependencies = arguments[0];
        factory = arguments[1];
    } else if (arguments.length == 1) {
        id = null;
        dependencies = [];
        factory = arguments[0];
    }

    var modules = [];
    for (var i=0; i<dependencies.length; i++) {
        var module_name = dependencies[i];

        var module = __defined__[module_name];

        org.quuux.huescript.Log.d(TAG, "cache %s for %s!!!", module ? "hit" : "miss", module_name);

        if (!module) {
            org.quuux.huescript.Log.d(TAG, "including -> %s", dependencies[i]);
            module = require(dependencies[i]).defined;
            __defined__[module_name] = module;
        }

        modules.push(module);
    }

    org.quuux.huescript.Log.d(TAG, "calling factory");
    exports.defined = factory.apply(this, modules);
    return exports.defined;
}

