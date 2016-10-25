
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
    emulatedModules[moduleId] = {};
    for (var i = 0; i < dependencies.length; ++i) {
        var dependencyName = dependencies[i];
        resolvedDependencies.push(emulatedModules[dependencyName === 'exports' ? moduleId : dependencyName]);
    }
    var result = body.apply(null, resolvedDependencies);
    if (result != null) {
        emulatedModules[moduleId] = result;
    }
}
define.amd = {};
