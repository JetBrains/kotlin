
    var emulatedModules = { kotlin: kotlin };
    var module = { exports: {} };

    function require(moduleId) {
        return emulatedModules[moduleId];
    }

    function __beginModule__() {
        module.exports = {};
    }

    function __endModule__(moduleId) {
        emulatedModules[moduleId] = module.exports;
    }

    function define(moduleId, dependencies, body) {
        var resolvedDependencies = [];
        for (var i = 0; i < dependencies.length; ++i) {
            resolvedDependencies.push(emulatedModules[dependencies[i]]);
        }
        emulatedModules[moduleId] = body.apply(null, resolvedDependencies);
    }
    define.amd = {};
