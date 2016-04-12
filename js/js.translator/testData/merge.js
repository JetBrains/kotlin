var mergedRoot = Kotlin;
for (var propertyName in stdlib) {
    Kotlin[propertyName] = stdlib[propertyName];
}
(function () {
    for (var i = 0; i < Kotlin.lazyInitClasses.length; ++i) {
        Kotlin.createDefinition(Kotlin.lazyInitClasses[i], mergedRoot);
    }
})();
