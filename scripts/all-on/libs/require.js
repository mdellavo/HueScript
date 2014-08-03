
function define() {

    var TAG = __file__.getName();

    var id, dependencies, factory;

    if (arguments.length == 3) {
        id = arguments[0];
        dependencies = arguments[1];
        factory = arguments[2];
    } else if (arguments.length == 2) {
        id = __file__.getName();
        dependencies = arguments[0];
        factory = arguments[1];
    } else if (arguments.length == 1) {
        id = __file__.getName();
        dependencies = [];
        factory = arguments[0];
    }

    org.quuux.huescript.Log.d(TAG, "id=%s / dependencies=%s / factory=%s", id, dependencies, factory);


    for (var defined in __defined__) {
        org.quuux.huescript.Log.d(TAG, "defined(%s) -> %s", defined, __defined__[defined]);
    }

    var modules = [];
    for (var i=0; i<dependencies.length; i++) {
        var module_name = dependencies[i];

        var module = __defined__[module_name];

        org.quuux.huescript.Log.d(TAG, "cache %s for %s!!!", module ? "hit" : "miss", module_name);

        if (!module) {
            org.quuux.huescript.Log.d(TAG, "require(%s)", module_name);
            module = require(module_name).defined;
            __defined__[module_name] = module;
        }

        modules.push(module);
    }

    org.quuux.huescript.Log.d(TAG, "calling factory for %s", module_name);
    exports.defined = factory.apply(this, modules);
    return exports.defined;
}

