
import foo = JS_TESTS.foo;
import varargInt = JS_TESTS.foo.varargInt;
import varargNullableInt = JS_TESTS.foo.varargNullableInt;
import varargWithOtherParameters = JS_TESTS.foo.varargWithOtherParameters;
import varargWithComplexType = JS_TESTS.foo.varargWithComplexType;
import sumNullable = JS_TESTS.foo.sumNullable;
import defaultParameters = JS_TESTS.foo.defaultParameters;
import generic1 = JS_TESTS.foo.generic1;
import generic2 = JS_TESTS.foo.generic2;
import generic3 = JS_TESTS.foo.generic3;
import inlineFun = JS_TESTS.foo.inlineFun;
import _const_val = JS_TESTS.foo._const_val;
import _val = JS_TESTS.foo._val;
import _var = JS_TESTS.foo._var;
import A = JS_TESTS.foo.A;
import A1 = JS_TESTS.foo.A1;
import A2 = JS_TESTS.foo.A2;
import A3 = JS_TESTS.foo.A3;
import _valCustom = JS_TESTS.foo._valCustom;
import _valCustomWithField = JS_TESTS.foo._valCustomWithField;
import A4 = JS_TESTS.foo.A4;
import O = JS_TESTS.foo.O;
import takesO = JS_TESTS.foo.takesO;
import KT_37829 = JS_TESTS.foo.KT_37829;
import TestSealed = JS_TESTS.foo.TestSealed;
import TestAbstract = JS_TESTS.foo.TestAbstract;
import TestDataClass = JS_TESTS.foo.TestDataClass;
import TestEnumClass = JS_TESTS.foo.TestEnumClass;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(foo.sum(10, 20) === 30);
    assert(varargInt(new Int32Array([1, 2, 3])) === 3);
    assert(varargNullableInt([10, 20, 30, null, undefined, 40]) === 6);
    assert(varargWithOtherParameters("1234", ["1", "2", "3"], "12") === 9);
    assert(varargWithComplexType([]) === 0);
    assert(varargWithComplexType([
        x => x,
        x => [new Int32Array([1, 2, 3])],
        x => [],
    ]) === 3);

    assert(sumNullable(10, null) === 10);
    assert(sumNullable(undefined, 20) === 20);
    assert(sumNullable(1, 2) === 3);
    assert(defaultParameters(20, "OK") === "20OK");
    assert(generic1<string>("FOO") === "FOO");
    assert(generic1({x: 10}).x === 10);
    assert(generic2(null) === true);
    assert(generic2(undefined) === true);
    assert(generic2(10) === false);
    assert(generic3(10, true, "__", {}) === null);

    let result: number = 0;
    inlineFun(10, x => { result = x; });
    assert(result === 10);

    assert(_const_val === 1);
    assert(_val === 1);
    assert(_var === 1);
    foo._var = 1000;
    assert(foo._var === 1000);

    assert(foo._valCustom === 1);
    assert(foo._valCustomWithField === 2);
    assert(foo._varCustom === 1);
    foo._varCustom = 20;
    assert(foo._varCustom === 1);
    assert(foo._varCustomWithField === 10);
    foo._varCustomWithField = 10;
    assert(foo._varCustomWithField === 1000);

    new A();
    assert(new A1(10).x === 10);
    assert(new A2("10", true).x === "10");
    assert(new A3().x === 100);


    const a4 = new A4();

    assert(a4._valCustom === 1);
    assert(a4._valCustomWithField === 2);
    assert(a4._varCustom === 1);
    a4._varCustom = 20;
    assert(a4._varCustom === 1);
    assert(a4._varCustomWithField === 10);
    a4._varCustomWithField = 10;
    assert(a4._varCustomWithField === 1000);

    assert(O.x === 10);
    assert(O.foo() === 20);
    assert(takesO(O) === 30);

    assert(KT_37829.Companion.x == 10);

    assert(new TestSealed.AA().name == "AA");
    assert(new TestSealed.AA().bar() == "bar");
    assert(new TestSealed.BB().name == "BB");
    assert(new TestSealed.BB().baz() == "baz");

    assert(new TestAbstract.AA().name == "AA");
    assert(new TestAbstract.AA().bar() == "bar");
    assert(new TestAbstract.BB().name == "BB");
    assert(new TestAbstract.BB().baz() == "baz");

    assert(new TestDataClass.Nested().prop == "hello");

    assert(TestEnumClass.A.foo == 0)
    assert(TestEnumClass.B.foo == 1)
    assert(TestEnumClass.A.bar("aBar") == "aBar")
    assert(TestEnumClass.B.bar("bBar") == "bBar")
    assert(TestEnumClass.A.bay() == "A")
    assert(TestEnumClass.B.bay() == "B")
    assert(TestEnumClass.A.constructorParameter == "aConstructorParameter")
    assert(TestEnumClass.B.constructorParameter == "bConstructorParameter")

    assert(TestEnumClass.valueOf("A") === TestEnumClass.A)
    assert(TestEnumClass.valueOf("B") === TestEnumClass.B)

    assert(TestEnumClass.values().indexOf(TestEnumClass.A) != -1)
    assert(TestEnumClass.values().indexOf(TestEnumClass.B) != -1)

    assert(new TestEnumClass.Nested().prop == "hello2")

    return "OK";
}