"use strict";
var producer = JS_TESTS.foo.producer;
var consumer = JS_TESTS.foo.consumer;
var A = JS_TESTS.foo.A;
var B = JS_TESTS.foo.B;
var childProducer = JS_TESTS.foo.childProducer;
var childConsumer = JS_TESTS.foo.childConsumer;
var genericChildProducer = JS_TESTS.foo.genericChildProducer;
var genericChildConsumer = JS_TESTS.foo.genericChildConsumer;
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
    var oneMoreNonExportedType = childProducer(322);
    assert(consumer(oneMoreNonExportedType) == 322);
    assert(childConsumer(oneMoreNonExportedType) == 322);
    return "OK";
}
