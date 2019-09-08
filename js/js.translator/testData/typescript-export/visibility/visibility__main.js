"use strict";
var Class = JS_TESTS.Class;
function box() {
    var tens = [
        JS_TESTS.publicVal,
        JS_TESTS.publicFun(),
        new JS_TESTS.Class().publicVal,
        new JS_TESTS.Class().publicFun()
    ];
    if (!tens.every(function (value) { return value === 10; }))
        return "Fail 1";
    if (!(new Class() instanceof Class))
        return "Fail 2";
    if (!(new Class.publicClass() instanceof Class.publicClass))
        return "Fail 3";
    return "OK";
}
