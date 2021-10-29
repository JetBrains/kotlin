import foo = JS_TESTS.foo;
import A1 = JS_TESTS.foo.A1;
import A2 = JS_TESTS.foo.A2;
import A3 = JS_TESTS.foo.A3;
import A4 = JS_TESTS.foo.A4;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(foo.invalid_args_name_sum(10, 20) === 30);
    assert((foo as any)["invalid@name sum"](10, 20) === 30);

    assert((foo as any)["invalid name val"] === 1);
    assert((foo as any)["invalid@name var"] === 1);
    (foo as any)["invalid@name var"] = 4
    assert((foo as any)["invalid@name var"] === 4);

    new (foo as any)["Invalid A"]();

    assert(new A1(10, 20)["first value"] === 10);
    assert(new A1(10, 20)["second.value"] === 20);

    assert(new A2()["invalid:name"] === 42);

    const a3 = new A3()
    assert(a3.invalid_args_name_sum(10, 20) === 30);
    assert(a3["invalid@name sum"](10, 20) === 30);

    assert(A4.Companion["@invalid+name@"] == 23);
    assert(A4.Companion["^)run.something.weird^("]() === ")_(");

    return "OK";
}