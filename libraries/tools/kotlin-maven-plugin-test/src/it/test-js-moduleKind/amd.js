var amdModules = {};
function define(moduleName, dependencies, body) {
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