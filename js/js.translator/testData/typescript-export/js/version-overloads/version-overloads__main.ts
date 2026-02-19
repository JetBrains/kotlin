import X = JS_TESTS.foo.X;
import foo2 = JS_TESTS.foo.foo2;

function box(): string {
    foo2(1, undefined, undefined, undefined, () => { });
    foo2(1, "a", "b", undefined, () => { });
    foo2(1, "a", "b", 1.0, () => { });

    let x = new X();
    x.foo(1);
    x.foo(1, "a", "b");
    x.foo(1, "a", "b", 1.0);

    return "OK";
}