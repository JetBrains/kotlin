var amdModules = {};
// Hard-code expected dependency order since we are unable to refer to modules by filename here.
var moduleNames = ["kotlin", "test-js-moduleKind", "check"];
function define(moduleName, dependencies, body) {
    if (Array.isArray(moduleName)) {
        body = dependencies;
        dependencies = moduleName;
        moduleName = moduleNames.shift();
    }
    else {
        if (moduleName !== moduleNames.shift()) throw new Error("Unexpected dependency")
    }
    var resolvedDependencies = [];
    var currentModule = {};
    amdModules[moduleName] = currentModule;
    for (var i = 0; i < dependencies.length; ++i) {
        var dependencyName = dependencies[i];
        var dependency = dependencyName === 'exports' ? currentModule : amdModules[dependencyName];
        resolvedDependencies.push(dependency);
    }
    body.apply(body, resolvedDependencies);
}
define.amd = {};