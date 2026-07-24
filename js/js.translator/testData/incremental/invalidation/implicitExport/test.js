module.exports = function (step) {
    var stdlib = require("./kotlin-kotlin-stdlib.js");
    var lib1 = require("./lib1.js");
    switch (step) {
        case 0:
            return lib1.consumeCollection(stdlib.kotlin.collections.KtMap.fromJsMap(new Map([['foo', 'OK']])));
        case 1:
            return lib1.consumeCollection(stdlib.kotlin.collections.KtList.fromJsArray(['OK']));
        default:
            return 'Fail';
    }
}
