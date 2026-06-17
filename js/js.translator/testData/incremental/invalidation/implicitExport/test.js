module.exports = function (step) {
    var stdlib = require("./kotlin-kotlin-stdlib.js");
    var lib1 = require("./lib1.js");
    switch (step) {
        case 0:
            return lib1.consumeCollection(stdlib.kotlin.collections.KtMap.fromJsMap(new Map([['foo', 'OK']])));
        case 1:
            // TODO(KT-70622): Delete this 'if'
            if (typeof stdlib.kotlin.collections.KtList === 'undefined') {
                return 'OK';
            }
            return lib1.consumeCollection(stdlib.kotlin.collections.KtList.fromJsArray(['OK']));
        default:
            return 'Fail';
    }
}
