(function(global) {
    var modules = {};

    function define(name, dependencies, body) {
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