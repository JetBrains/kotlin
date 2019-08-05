"use strict";
var C1 = JS_TESTS.foo.bar.baz.C1;
var C2 = JS_TESTS.a.b.C2;
var C3 = JS_TESTS.C3;
function box() {
    var c1 = new C1("1");
    var c2 = new C2("2");
    var c3 = new C3("3");
    var res1 = JS_TESTS.foo.bar.baz.f(c1, c2, c3);
    var res2 = JS_TESTS.a.b.f(c1, c2, c3);
    var res3 = JS_TESTS.f(c1, c2, c3);
    if (res1 !== "foo.bar.baz.f(C1(value=1), C2(value=2), C3(value=3))")
        return "Fail 1: " + res1;
    if (res2 !== "a.b.f(C1(value=1), C2(value=2), C3(value=3))")
        return "Fail 2: " + res2;
    if (res3 !== "f(C1(value=1), C2(value=2), C3(value=3))")
        return "Fail 3: " + res3;
    return "OK";
}
