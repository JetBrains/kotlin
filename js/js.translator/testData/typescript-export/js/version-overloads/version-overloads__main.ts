import A = JS_TESTS.foo.A;
import B = JS_TESTS.foo.B;
import C = JS_TESTS.foo.C;
import D = JS_TESTS.foo.D;
import X = JS_TESTS.foo.X;
import foo2 = JS_TESTS.foo.foo2;
import mid2 = JS_TESTS.foo.mid2;

function box(): string {
    let a1 = new A(1);
    let a2 = new A(1, "a", "b");
    let a3 = new A(1, "a", "b", 1.0);

    let b1 = new B(1);
    let b2 = new B(1, "b", 1.0);
    let b3 = new B(1, "a", "b", 1.0);

    let c1 = new C(1);
    let c2 = new C(1, "a");
    let c3 = new C(1, "a", 1.0);

    let d1 = new D(1);
    let d2 = new D(1, "a");
    let d3 = new D(1, 2, "a");

    foo2(1, undefined, undefined, undefined, () => { });
    foo2(1, "a", "b", undefined, () => { });
    foo2(1, "a", "b", 1.0, () => { });
    mid2(1, undefined, undefined, undefined, () => { });
    mid2(1, undefined, "a", 1.0, () => { });
    mid2(1, 2, "b", 1.0, () => { });

    let x = new X();
    x.foo(1);
    x.foo(1, "a", "b");
    x.foo(1, "a", "b", 1.0);
    x.mid(1);
    x.mid(1, "a", 1.0);
    x.mid(1, 2, "b", 1.0);

    return "OK";
}