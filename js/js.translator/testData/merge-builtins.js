var require = (function () {
    var builtins = module.exports.kotlin;
    var propertyNames = Object.getOwnPropertyNames(builtins);
    Kotlin.kotlin = Kotlin.kotlin || {};
    for (var i = 0; i < propertyNames.length; ++i) {
        var propertyName = propertyNames[i];
        Kotlin.kotlin[propertyName] = builtins[propertyName];
    }
    return function() {
        return Kotlin;
    }
})();