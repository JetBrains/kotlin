"use strict";
var foo = JS_TESTS.foo;
function assert(condition, message) {
    if (message === void 0) { message = "Assertion failed"; }
    if (!condition) {
        throw message;
    }
}
function box() {
    assert(foo.foo === "Foo", "Error in property 'foo'");
    assert(foo.bar() === "Bar", "Error in property 'bar'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.baz === undefined, "Error in property 'baz'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.inter === undefined, "Error in method 'inter'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.NotExportableNestedInsideInterface === undefined, "Error in class 'NotExportableNestedInsideInterface'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.Companion === undefined, "Error in object 'Companion'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.notExportableReified === undefined, "Error in method 'notExportableReified'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.notExportableSuspend === undefined, "Error in method 'notExportableSuspend'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.notExportableReturn === undefined, "Error in method 'notExportableReturn'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.notExportableExentsionProperty === undefined, "Error in method 'notExportableExentsionProperty'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.NotExportableAnnotation === undefined, "Error in nested class 'NotExportableAnnotation'");
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(foo.NotExportableInlineClass === undefined, "Error in nested class 'NotExportableInlineClass'");
    return "OK";
}
