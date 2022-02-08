"use strict";
var producer = JS_TESTS.foo.producer;
var consumer = JS_TESTS.foo.consumer;
var A = JS_TESTS.foo.A;
var B = JS_TESTS.foo.B;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    var nonExportedType = producer(42);
    var a = new A(nonExportedType);
    var b = new B(43);
    assert(consumer(nonExportedType) == 42);
    a.value = producer(24);
    assert(consumer(b) == 43);
    assert(consumer(a.value) == 24);
    assert(consumer(a.increment(nonExportedType)) == 43);
    return "OK";
}
