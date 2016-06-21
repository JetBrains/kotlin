(function () {
    var stdlib = module.exports;
    Object.getOwnPropertyNames(stdlib).forEach(function(propertyName) {
        Kotlin[propertyName] = stdlib[propertyName];
    });
})();
