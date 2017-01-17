
var emulatedModules = { kotlin: kotlin };
var module = { exports: {} };

// TODO don't expose by default when run test with AMD module kind
function require(moduleId) {
    return emulatedModules[moduleId];
}

var $kotlin_test_internal$ = {
    require: require,
    beginModule : function () {
        module.exports = {};
    },
    endModule : function(moduleId) {
        emulatedModules[moduleId] = module.exports;
    }
};

// TODO expose only when run test with AMD or UMD module kind
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
