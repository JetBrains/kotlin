import TestAbstract = JS_TESTS.foo.TestAbstract;
import Euro = JS_TESTS.foo.Euro;
import Money = JS_TESTS.foo.Money;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(new TestAbstract.AA().name == "AA");
    assert(new TestAbstract.AA().bar() == "bar");
    assert(new TestAbstract.BB().name == "BB");
    assert(new TestAbstract.BB().baz() == "baz");

    const euro = new Euro(44);

    assert(!euro.isZero());
    assert(euro.amount === 44);
    assert(euro instanceof Euro);
    assert(euro instanceof Money);

    return "OK";
}