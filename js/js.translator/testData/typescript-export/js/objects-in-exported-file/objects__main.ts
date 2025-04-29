import O = JS_TESTS.foo.O;
import O0 = JS_TESTS.foo.O0;
import Parent = JS_TESTS.foo.Parent;
import takesO = JS_TESTS.foo.takesO;
import getParent = JS_TESTS.foo.getParent;
import createNested1 = JS_TESTS.foo.createNested1;
import createNested2 = JS_TESTS.foo.createNested2;
import createNested3 = JS_TESTS.foo.createNested3;
import WithSimpleObjectInside = JS_TESTS.foo.WithSimpleObjectInside;
import SimpleObjectWithInterface1 = JS_TESTS.foo.SimpleObjectWithInterface1;
import Interface1 = JS_TESTS.foo.Interface1;
import SimpleObjectWithBothInterfaces = JS_TESTS.foo.SimpleObjectWithBothInterfaces;
import Interface2 = JS_TESTS.foo.Interface2;
import SimpleObjectInheritingAbstract = JS_TESTS.foo.SimpleObjectInheritingAbstract;
import SimpleObjectInheritingAbstractAndInterface1 = JS_TESTS.foo.SimpleObjectInheritingAbstractAndInterface1;
import SimpleObjectInheritingAbstractAndBothInterfaces = JS_TESTS.foo.SimpleObjectInheritingAbstractAndBothInterfaces;
import SimpleObjectWithInterface1AndNested = JS_TESTS.foo.SimpleObjectWithInterface1AndNested;
import SimpleObjectWithBothInterfacesAndNested = JS_TESTS.foo.SimpleObjectWithBothInterfacesAndNested;
import SimpleObjectInheritingAbstractAndNested = JS_TESTS.foo.SimpleObjectInheritingAbstractAndNested;
import SimpleObjectInheritingAbstractAndInterface1AndNested = JS_TESTS.foo.SimpleObjectInheritingAbstractAndInterface1AndNested;
import SimpleObjectInheritingAbstractAndBothInterfacesAndNested = JS_TESTS.foo.SimpleObjectInheritingAbstractAndBothInterfacesAndNested;
import Zero = JS_TESTS.foo.Zero;
import Money = JS_TESTS.foo.Money;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(typeof O0 === "object" && O0 != null);
    assert(O.x === 10);
    assert(O.foo() === 20);
    assert(takesO(O) === 30)

    // Do not strip types from those test cases (it is a check of nested objects types usability)
    const parent: typeof Parent = Parent
    const nested1: typeof Parent.Nested1 = Parent.Nested1
    const nested2: Parent.Nested1.Nested2 = new Parent.Nested1.Nested2()
    const nested3: Parent.Nested1.Nested2.Companion.Nested3 = new Parent.Nested1.Nested2.Companion.Nested3()

    assert(nested1.value === "Nested1")
    assert(getParent() === parent)
    assert(createNested1() === nested1)
    assert(createNested2() !== nested2 && createNested2() instanceof Parent.Nested1.Nested2)
    assert(createNested3() !== nested3 && createNested3() instanceof Parent.Nested1.Nested2.Companion.Nested3)

    assert(WithSimpleObjectInside.value === "WithSimpleObjectInside");
    assert(WithSimpleObjectInside.SimpleObject.value === "SimpleObject");

    assert(SimpleObjectWithInterface1.foo() === "OK");
    assert((SimpleObjectWithInterface1 as Interface1).foo() === "OK");

    assert(SimpleObjectWithBothInterfaces.foo() === "OK");
    assert(SimpleObjectWithBothInterfaces.bar() === "OK");
    assert((SimpleObjectWithBothInterfaces as Interface1).foo() === "OK");
    assert((SimpleObjectWithBothInterfaces as Interface2).bar() === "OK");

    assert(SimpleObjectInheritingAbstract instanceof JS_TESTS.foo.BaseWithCompanion);

    assert(SimpleObjectInheritingAbstractAndInterface1 instanceof JS_TESTS.foo.BaseWithCompanion);
    assert(SimpleObjectInheritingAbstractAndInterface1.foo() === "OK");
    assert((SimpleObjectInheritingAbstractAndInterface1 as Interface1).foo() === "OK");

    assert(SimpleObjectInheritingAbstractAndBothInterfaces instanceof JS_TESTS.foo.BaseWithCompanion);
    assert(SimpleObjectInheritingAbstractAndBothInterfaces.foo() === "OK");
    assert(SimpleObjectInheritingAbstractAndBothInterfaces.bar() === "OK");
    assert((SimpleObjectInheritingAbstractAndBothInterfaces as Interface1).foo() === "OK");
    assert((SimpleObjectInheritingAbstractAndBothInterfaces as Interface2).bar() === "OK");

    assert(SimpleObjectWithInterface1AndNested.foo() === "OK");
    assert((SimpleObjectWithInterface1AndNested as Interface1).foo() === "OK");

    assert(SimpleObjectWithBothInterfacesAndNested.foo() === "OK");
    assert(SimpleObjectWithBothInterfacesAndNested.bar() === "OK");
    assert((SimpleObjectWithBothInterfacesAndNested as Interface1).foo() === "OK");
    assert((SimpleObjectWithBothInterfacesAndNested as Interface2).bar() === "OK");

    assert(SimpleObjectInheritingAbstractAndNested instanceof JS_TESTS.foo.BaseWithCompanion);

    assert(SimpleObjectInheritingAbstractAndInterface1AndNested instanceof JS_TESTS.foo.BaseWithCompanion);
    assert(SimpleObjectInheritingAbstractAndInterface1AndNested.foo() === "OK");
    assert((SimpleObjectInheritingAbstractAndInterface1AndNested as Interface1).foo() === "OK");

    assert(SimpleObjectInheritingAbstractAndBothInterfacesAndNested instanceof JS_TESTS.foo.BaseWithCompanion);
    assert(SimpleObjectInheritingAbstractAndBothInterfacesAndNested.foo() === "OK");
    assert(SimpleObjectInheritingAbstractAndBothInterfacesAndNested.bar() === "OK");
    assert((SimpleObjectInheritingAbstractAndBothInterfacesAndNested as Interface1).foo() === "OK");
    assert((SimpleObjectInheritingAbstractAndBothInterfacesAndNested as Interface2).bar() === "OK");

    assert(Zero instanceof Money);
    assert(Zero.amount === 0);

    return "OK";
}