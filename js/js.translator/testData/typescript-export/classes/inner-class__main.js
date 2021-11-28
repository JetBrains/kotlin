"use strict";
var TestInner = JS_TESTS.foo.TestInner;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    var outer = new TestInner("Hello ");
    var inner = new outer.Inner("World");
    var innerFromNumber = outer.Inner.fromNumber(1654);
    var secondInner = new inner.SecondLayerInner("!");
    assert(outer instanceof TestInner);
    assert(inner instanceof TestInner.Inner);
    assert(innerFromNumber instanceof TestInner.Inner);
    assert(secondInner instanceof TestInner.Inner.SecondLayerInner);
    assert(inner.a == "World");
    assert(inner.concat == "Hello World");
    assert(innerFromNumber.a == "1654");
    assert(innerFromNumber.concat == "Hello 1654");
    assert(secondInner.a == "!");
    assert(secondInner.concat == "Hello World!");
    return "OK";
}
