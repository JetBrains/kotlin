"use strict";
var foo = JS_TESTS.foo;
function box() {
    var tens = [
        foo.exportedVal,
        foo.exportedFun(),
        new foo.ExportedClass().value,
        foo.fileLevelExportedVal,
        foo.fileLevelExportedFun(),
        new foo.FileLevelExportedClass().value
    ];
    if (tens.every(function (value) { return value === 10; }))
        return "OK";
    return "FAIL";
}
