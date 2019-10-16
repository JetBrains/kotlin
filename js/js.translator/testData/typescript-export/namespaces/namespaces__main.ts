import C1 = JS_TESTS.foo.bar.baz.C1;
import C2 = JS_TESTS.a.b.C2;
import C3 = JS_TESTS.C3;

function box(): string {
    const c1 = new C1("1");
    const c2 = new C2("2");
    const c3 = new C3("3");

    const res1 = JS_TESTS.foo.bar.baz.f(c1, c2, c3);
    const res2 = JS_TESTS.a.b.f(c1, c2, c3);
    const res3 = JS_TESTS.f(c1, c2, c3);

    if (res1 !== "foo.bar.baz.f(C1(value=1), C2(value=2), C3(value=3))")
        return "Fail 1: " + res1;

    if (res2 !== "a.b.f(C1(value=1), C2(value=2), C3(value=3))")
        return "Fail 2: " + res2;

    if (res3 !== "f(C1(value=1), C2(value=2), C3(value=3))")
        return "Fail 3: " + res3;

    return "OK";
}