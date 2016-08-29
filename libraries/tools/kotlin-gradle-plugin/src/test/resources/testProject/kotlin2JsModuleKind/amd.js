var amdModules = {};
function define(moduleName, dependencies, body) {
    var resolvedDependencies = [];
    for (var i = 0; i < dependencies.length; ++i) {
        resolvedDependencies.push(amdModules[dependencies[i]]);
    }
    amdModules[moduleName] = body.apply(body, resolvedDependencies);
}
define.amd = {};