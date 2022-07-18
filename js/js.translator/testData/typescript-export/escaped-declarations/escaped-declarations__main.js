"use strict";
var foo = JS_TESTS.foo;
var A1 = JS_TESTS.foo.A1;
var A2 = JS_TESTS.foo.A2;
var A3 = JS_TESTS.foo.A3;
var A4 = JS_TESTS.foo.A4;
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(foo.invalid_args_name_sum(10, 20) === 30);
    assert(foo["invalid@name sum"](10, 20) === 30);
    assert(foo["invalid name val"] === 1);
    assert(foo["invalid@name var"] === 1);
    foo["invalid@name var"] = 4;
    assert(foo["invalid@name var"] === 4);
    new foo["Invalid A"]();
    assert(new A1(10, 20)["first value"] === 10);
    assert(new A1(10, 20)["second.value"] === 20);
    assert(new A2()["invalid:name"] === 42);
    var a3 = new A3();
    assert(a3.invalid_args_name_sum(10, 20) === 30);
    assert(a3["invalid@name sum"](10, 20) === 30);
    assert(A4.Companion["@invalid+name@"] == 23);
    assert(A4.Companion["^)run.something.weird^("]() === ")_(");
    return "OK";
}
