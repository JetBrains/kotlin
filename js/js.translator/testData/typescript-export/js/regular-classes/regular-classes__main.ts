import A = JS_TESTS.foo.A;
import A1 = JS_TESTS.foo.A1;
import A2 = JS_TESTS.foo.A2;
import A3 = JS_TESTS.foo.A3;
import A4 = JS_TESTS.foo.A4;
import A5 = JS_TESTS.foo.A5;
import A6 = JS_TESTS.foo.A6;
import GenericClassWithConstraint = JS_TESTS.foo.GenericClassWithConstraint;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    new A();
    assert(new A1(10).x === 10);
    assert(new A2("10", true).x === "10");
    assert(new A3().x === 100);
    assert(new A4("Hello").test() === "Hello");
    assert(A5.Companion.x === 10);
    assert(new A6().then() === 42);
    assert(new A6().catch() === 24);
    assert(new GenericClassWithConstraint(new A6()).test.catch() === 24)

    return "OK";
}