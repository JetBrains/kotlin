import A = JS_TESTS.foo.A;
import C = JS_TESTS.foo.C;
import X = JS_TESTS.foo.X;
import foo2 = JS_TESTS.foo.foo2;

function box(): string {
    let a1 = new A(1);
    let a2 = new A(1, "a", "b");
    let a3 = new A(1, "a", "b", 1.0);

    let c1 = new C(1);
    let c2 = new C(1, "a");
    let c3 = new C(1, "a", 1.0);

    foo2(1, undefined, undefined, undefined, () => { });
    foo2(1, "a", "b", undefined, () => { });
    foo2(1, "a", "b", 1.0, () => { });

    let x = new X();
    x.foo(1);
    x.foo(1, "a", "b");
    x.foo(1, "a", "b", 1.0);

    return "OK";
}