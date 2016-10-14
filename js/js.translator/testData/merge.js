(function () {
    var stdlib = module.exports;
    function copyProperties(from, to) {
        var propertyNames = Object.getOwnPropertyNames(from);
        for (var i = 0; i < propertyNames.length; ++i) {
            var propertyName = propertyNames[i];
            if (propertyName in to) {
                copyProperties(from[propertyName], to[propertyName]);
            }
            else {
                to[propertyName] = from[propertyName];
            }
        }
    }
    copyProperties(stdlib, Kotlin);
})();
