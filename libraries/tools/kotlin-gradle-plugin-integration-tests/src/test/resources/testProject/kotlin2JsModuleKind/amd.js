(function(global) {
    var modules = {};

    // Hard-code expected dependency order since we are unable to refer to modules by filename here.
    var names = ["kotlin", "app", "check"];

    function define(name, dependencies, body) {
        if (Array.isArray(name)) {
            body = dependencies;
            dependencies = name;
            name = names.shift();
        }
        else {
            if (name !== names.shift()) throw new Error("Unexpected dependency")
        }
        var resolvedDependencies = [];
        var currentModule = {};
        modules[name] = currentModule;
        for (var i = 0; i < dependencies.length; ++i) {
            var dependencyName = dependencies[i];
            resolvedDependencies[i] = dependencyName === 'exports' ? currentModule : modules[dependencyName];
        }
        currentModule = body.apply(body, resolvedDependencies);
        if (currentModule) {
            modules[name] = currentModule;
        }
    }
    define.amd = {};

    global.define = define;
})(this);