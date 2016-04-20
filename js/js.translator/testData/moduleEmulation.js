(function(global) {
    var modules = { kotlin: kotlin };
    var module = { exports: {} };

    function require(moduleId) {
        return modules[moduleId];
    }

    function beginModule() {
        module.exports = {};
    }

    function endModule(moduleId) {
        modules[moduleId] = module.exports;
    }

    function define(moduleId, dependencies, body) {
        var resolvedDependencies = [];
        for (var i = 0; i < dependencies.length; ++i) {
            resolvedDependencies.push(modules[dependencies[i]]);
        }
        modules[moduleId] = body.apply(null, resolvedDependencies);
    }

    global.require = require;
    global.define = define;
    global.__beginModule__ = beginModule;
    global.__endModule__ = endModule;
    global.module = module;
})(this);