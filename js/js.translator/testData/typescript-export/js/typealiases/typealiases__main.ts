// Runtime declarations used to produce values for the aliases below.
import SomeClass = JS_TESTS.foo.SomeClass;
import GenericClass = JS_TESTS.foo.GenericClass;
import TwoGenericParamsClass = JS_TESTS.foo.TwoGenericParamsClass;
import SomeEnum = JS_TESTS.foo.SomeEnum;
import ClassUsingAlias = JS_TESTS.foo.ClassUsingAlias;
import consumeMyInt = JS_TESTS.foo.consumeMyInt;
import consumeCallback = JS_TESTS.foo.consumeCallback;
import consumeGenericAlias = JS_TESTS.foo.consumeGenericAlias;
import acceptInnerAlias = JS_TESTS.foo.acceptInnerAlias;
import ClassWithNestedTypealiases = JS_TESTS.foo.ClassWithNestedTypealiases;
import GenericClassWithNestedTypealiases = JS_TESTS.foo.GenericClassWithNestedTypealiases;
import EnumWithNestedTypealiases = JS_TESTS.foo.EnumWithNestedTypealiases;
import ObjectWithNestedTypealiases = JS_TESTS.foo.ObjectWithNestedTypealiases;
import ClassWithCompanionTypealiases = JS_TESTS.foo.ClassWithCompanionTypealiases;
import ClassWithNamedCompanionTypealiases = JS_TESTS.foo.ClassWithNamedCompanionTypealiases;

// --- Primitive type aliases ---
import MyInt = JS_TESTS.foo.MyInt;
import MyString = JS_TESTS.foo.MyString;
import MyBoolean = JS_TESTS.foo.MyBoolean;
import MyDouble = JS_TESTS.foo.MyDouble;
import MyByte = JS_TESTS.foo.MyByte;
import MyShort = JS_TESTS.foo.MyShort;
import MyFloat = JS_TESTS.foo.MyFloat;
import MyChar = JS_TESTS.foo.MyChar;
import MyAny = JS_TESTS.foo.MyAny;

// --- Nullable type aliases ---
import NullableInt = JS_TESTS.foo.NullableInt;
import NullableString = JS_TESTS.foo.NullableString;
import NullableAny = JS_TESTS.foo.NullableAny;

// --- Unsigned type aliases ---
import MyUByte = JS_TESTS.foo.MyUByte;
import MyUShort = JS_TESTS.foo.MyUShort;
import MyUInt = JS_TESTS.foo.MyUInt;

// --- Array type aliases ---
import MyIntArray = JS_TESTS.foo.MyIntArray;
import MyBooleanArray = JS_TESTS.foo.MyBooleanArray;
import MyStringArray = JS_TESTS.foo.MyStringArray;
import MyNullableIntArray = JS_TESTS.foo.MyNullableIntArray;
import NestedArray = JS_TESTS.foo.NestedArray;

// --- Class/Interface/Enum/Object type aliases ---
import AliasSomeClass = JS_TESTS.foo.AliasSomeClass;
import AliasSomeInterface = JS_TESTS.foo.AliasSomeInterface;
import AliasSomeExternalInterface = JS_TESTS.foo.AliasSomeExternalInterface;
import AliasSomeEnum = JS_TESTS.foo.AliasSomeEnum;
import NullableSomeClass = JS_TESTS.foo.NullableSomeClass;

// --- Generic type aliases (non-generic alias pointing to a concrete generic type) ---
import ConcreteGenericClass = JS_TESTS.foo.ConcreteGenericClass;
import ConcreteGenericClassInt = JS_TESTS.foo.ConcreteGenericClassInt;
import ConcreteTwoGenericParamsClass = JS_TESTS.foo.ConcreteTwoGenericParamsClass;

// --- Generic type aliases (alias with type parameters) ---
import AliasGenericClass = JS_TESTS.foo.AliasGenericClass;
import AliasTwoGenericParamsClass = JS_TESTS.foo.AliasTwoGenericParamsClass;
import FlippedTwoGenericParamsClass = JS_TESTS.foo.FlippedTwoGenericParamsClass;
import PartiallySpecializedGenericClass = JS_TESTS.foo.PartiallySpecializedGenericClass;

// --- Nullable generic type aliases ---
import NullableGenericClass = JS_TESTS.foo.NullableGenericClass;
import GenericClassNullableParam = JS_TESTS.foo.GenericClassNullableParam;

// --- Function type aliases ---
import SimpleCallback = JS_TESTS.foo.SimpleCallback;
import IntToString = JS_TESTS.foo.IntToString;
import BinaryOperation = JS_TESTS.foo.BinaryOperation;
import GenericTransformer = JS_TESTS.foo.GenericTransformer;
import NullableCallback = JS_TESTS.foo.NullableCallback;
import CallbackWithNullableParam = JS_TESTS.foo.CallbackWithNullableParam;
import HigherOrderFunction = JS_TESTS.foo.HigherOrderFunction;
import CurriedFunction = JS_TESTS.foo.CurriedFunction;
import SuspendCallback = JS_TESTS.foo.SuspendCallback;
import SuspendTransformer = JS_TESTS.foo.SuspendTransformer;

// --- Typealias to typealias ---
import AliasOfMyInt = JS_TESTS.foo.AliasOfMyInt;
import AliasOfAliasSomeClass = JS_TESTS.foo.AliasOfAliasSomeClass;
import AliasOfGenericAlias = JS_TESTS.foo.AliasOfGenericAlias;

// --- Throwable / Nothing / Unit type aliases ---
import MyThrowable = JS_TESTS.foo.MyThrowable;
import NullableThrowable = JS_TESTS.foo.NullableThrowable;
import MyNothing = JS_TESTS.foo.MyNothing;
import MyUnit = JS_TESTS.foo.MyUnit;

// --- Aliases with variance / inner class ---
import AliasWithVarianceInAliasedType = JS_TESTS.foo.AliasWithVarianceInAliasedType;
import AliasWithVarianceInsideOfAliasedType = JS_TESTS.foo.AliasWithVarianceInsideOfAliasedType;
import AliasWithInnerClass = JS_TESTS.foo.AliasWithInnerClass;

// --- Nested typealiases (inside classes/interfaces/enums, reachable via public namespaces) ---
import NestedInt = JS_TESTS.foo.ClassWithNestedTypealiases.NestedInt;
import NestedString = JS_TESTS.foo.ClassWithNestedTypealiases.NestedString;
import NestedClassAlias = JS_TESTS.foo.ClassWithNestedTypealiases.NestedClassAlias;
import NestedGenericAlias = JS_TESTS.foo.ClassWithNestedTypealiases.NestedGenericAlias;
import NestedConcreteGenericAlias = JS_TESTS.foo.ClassWithNestedTypealiases.NestedConcreteGenericAlias;
import NestedCallback = JS_TESTS.foo.ClassWithNestedTypealiases.NestedCallback;
import NestedNullable = JS_TESTS.foo.ClassWithNestedTypealiases.NestedNullable;
import ClassSelf = JS_TESTS.foo.ClassWithNestedTypealiases.Self;
import GenericNestedGenericAlias = JS_TESTS.foo.GenericClassWithNestedTypealiases.NestedGenericAlias;
import GenericSelf = JS_TESTS.foo.GenericClassWithNestedTypealiases.Self;
import InterfaceNestedInt = JS_TESTS.foo.InterfaceWithNestedTypealiases.InterfaceNestedInt;
import InterfaceNestedClassAlias = JS_TESTS.foo.InterfaceWithNestedTypealiases.InterfaceNestedClassAlias;
import InterfaceNestedGenericAlias = JS_TESTS.foo.InterfaceWithNestedTypealiases.InterfaceNestedGenericAlias;
import InterfaceNestedCallback = JS_TESTS.foo.InterfaceWithNestedTypealiases.InterfaceNestedCallback;
import InterfaceSelf = JS_TESTS.foo.InterfaceWithNestedTypealiases.Self;
import EnumNestedInt = JS_TESTS.foo.EnumWithNestedTypealiases.EnumNestedInt;
import EnumNestedClassAlias = JS_TESTS.foo.EnumWithNestedTypealiases.EnumNestedClassAlias;
import EnumSelf = JS_TESTS.foo.EnumWithNestedTypealiases.Self;
import OpenClassNestedAlias = JS_TESTS.foo.OpenClassWithNestedTypealiases.OpenClassNestedAlias;
import AbstractNestedAlias = JS_TESTS.foo.AbstractClassWithNestedTypealiases.AbstractNestedAlias;
import OuterNestedAlias = JS_TESTS.foo.OuterWithNested.OuterNestedAlias;
import DeeplyNestedAlias = JS_TESTS.foo.OuterWithNested.InnerNested.DeeplyNestedAlias;
import DeeplyNestedClassAlias = JS_TESTS.foo.OuterWithNested.InnerNested.DeeplyNestedClassAlias;
import SealedNestedAlias = JS_TESTS.foo.SealedClassWithNestedTypealiases.SealedNestedAlias;
import SealedInterfaceNestedAlias = JS_TESTS.foo.SealedInterfaceWithNestedTypealiases.SealedInterfaceNestedAlias;
import VisibleAlias = JS_TESTS.foo.ClassWithIgnoredNestedTypealias.VisibleAlias;

import ObjectSelf = JS_TESTS.foo.ObjectWithNestedTypealiases.Self;
import CompanionSelf = JS_TESTS.foo.ClassWithCompanionTypealiases.Companion.Self;
import NamedCompanionSelf = JS_TESTS.foo.ClassWithNamedCompanionTypealiases.Named.Self;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    // // --- Primitive aliases ---
    const myInt: MyInt = 42;
    const myString: MyString = "str";
    const myBoolean: MyBoolean = true;
    const myDouble: MyDouble = 3.5;
    const myByte: MyByte = 1;
    const myShort: MyShort = 2;
    const myFloat: MyFloat = 4.5;
    const myChar: MyChar = 99 as MyChar;
    const myAny: MyAny = {};
    assert(myInt === 42);
    assert(myString === "str");
    assert(myBoolean === true);
    assert(myDouble === 3.5);
    assert(myByte + myShort === 3);
    assert(myFloat === 4.5);
    assert(myChar !== null);
    assert(myAny !== null);

    // --- Nullable aliases ---
    const nullableInt: NullableInt = null;
    const nullableString: NullableString = "x";
    const nullableAny: NullableAny = undefined;
    assert(nullableInt === null);
    assert(nullableString === "x");
    assert(nullableAny === undefined);

    // --- Unsigned aliases ---
    const uByte: MyUByte = 1 as MyUByte;
    const uShort: MyUShort = 2 as MyUShort;
    const uInt: MyUInt = 3 as MyUInt;
    assert(uByte !== null && uShort !== null && uInt !== null);

    // --- Array aliases ---
    const intArray: MyIntArray = new Int32Array([1, 2, 3]);
    const booleanArray: MyBooleanArray = [true, false];
    const stringArray: MyStringArray = ["a", "b"];
    const nullableIntArray: MyNullableIntArray = null;
    const nestedArray: NestedArray = [["a"], ["b", "c"]];
    assert(intArray.length === 3);
    assert(booleanArray !== null);
    assert(stringArray[0] === "a");
    assert(nullableIntArray === null);
    assert(nestedArray[1][1] === "c");

    // --- Class / enum aliases ---
    const aliasSomeClass: AliasSomeClass = new SomeClass("value");
    assert(aliasSomeClass.value === "value");
    const nullableSomeClass: NullableSomeClass = null;
    assert(nullableSomeClass === null);
    const aliasSomeEnum: AliasSomeEnum = SomeEnum.B;
    assert(aliasSomeEnum.name === "B");
    // Interface aliases are used purely in type positions (branded interfaces are not implementable).
    let aliasSomeInterface: AliasSomeInterface;
    let aliasSomeExternalInterface: AliasSomeExternalInterface;

    // --- Concrete generic aliases ---
    const concreteGeneric: ConcreteGenericClass = new GenericClass<string>("g");
    assert(concreteGeneric.value === "g");
    const concreteGenericInt: ConcreteGenericClassInt = new GenericClass<number>(7);
    assert(concreteGenericInt.value === 7);
    const concreteTwo: ConcreteTwoGenericParamsClass = new TwoGenericParamsClass<string, number>("k", 9);
    assert(concreteTwo.first === "k" && concreteTwo.second === 9);

    // --- Parameterized generic aliases ---
    const aliasGeneric: AliasGenericClass<string> = new GenericClass<string>("ag");
    assert(aliasGeneric.value === "ag");
    const aliasTwo: AliasTwoGenericParamsClass<string, number> = new TwoGenericParamsClass<string, number>("f", 1);
    assert(aliasTwo.first === "f" && aliasTwo.second === 1);
    const flipped: FlippedTwoGenericParamsClass<string, number> = new TwoGenericParamsClass<number, string>(1, "f");
    assert(flipped.first === 1 && flipped.second === "f");
    const partial: PartiallySpecializedGenericClass<number> = new TwoGenericParamsClass<string, number>("p", 2);
    assert(partial.first === "p" && partial.second === 2);

    // --- Nullable generic aliases ---
    const nullableGeneric: NullableGenericClass<string> = null;
    assert(nullableGeneric === null);
    const genericNullableParam: GenericClassNullableParam<string> = new GenericClass<string | null | undefined>(null);
    assert(genericNullableParam.value === null);

    // --- Function type aliases ---
    const simpleCallback: SimpleCallback = () => {};
    simpleCallback();
    const intToString: IntToString = x => "" + x;
    assert(intToString(5) === "5");
    const binaryOperation: BinaryOperation = (a, b) => a + b;
    assert(binaryOperation(2, 3) === 5);
    const transformer: GenericTransformer<number, string> = x => "#" + x;
    assert(transformer(1) === "#1");
    const nullableCallback: NullableCallback = null;
    assert(nullableCallback === null);
    const callbackWithNullableParam: CallbackWithNullableParam = x => (x === null ? null : "" + x);
    assert(callbackWithNullableParam(null) === null);
    assert(callbackWithNullableParam(4) === "4");
    const higherOrder: HigherOrderFunction = f => f(3).length;
    assert(higherOrder(x => "abc") === 3);
    const curried: CurriedFunction = a => b => a > 0 && b.length > 0;
    assert(curried(1)("x") === true);
    const suspendCallback: SuspendCallback = () => Promise.resolve();
    assert(suspendCallback() instanceof Promise);
    const suspendTransformer: SuspendTransformer<number, string> = x => Promise.resolve("" + x);
    assert(suspendTransformer(1) instanceof Promise);

    // --- Alias-to-alias chains ---
    const aliasOfMyInt: AliasOfMyInt = 8;
    assert(aliasOfMyInt === 8);
    const aliasOfAliasSomeClass: AliasOfAliasSomeClass = new SomeClass("chain");
    assert(aliasOfAliasSomeClass.value === "chain");
    const aliasOfGenericAlias: AliasOfGenericAlias<string> = new GenericClass<string>("gg");
    assert(aliasOfGenericAlias.value === "gg");

    // --- Throwable / Nothing / Unit ---
    const myThrowable: MyThrowable = new Error("e");
    assert(myThrowable.message === "e");
    const nullableThrowable: NullableThrowable = null;
    assert(nullableThrowable === null);
    let myNothing: MyNothing; // `never` — type-only usage.
    const myUnit: MyUnit = undefined;
    assert(myUnit === undefined);

    // --- Aliases with variance / inner class ---
    const varianceInAlias: AliasWithVarianceInAliasedType<string> = new GenericClass<string>("v");
    assert(varianceInAlias.value === "v");
    const varianceInsideAlias: AliasWithVarianceInsideOfAliasedType<string> = new GenericClass<string>("w") as any;
    assert(varianceInsideAlias.value === "w");
    let innerAlias: AliasWithInnerClass<number, string>; // inner-class alias — type-only usage.

    // --- Exported functions/classes that consume aliases ---
    assert(consumeMyInt(1) === 2);
    assert(consumeCallback(() => {}) === undefined);
    assert(consumeCallback(null) === null);
    assert(consumeGenericAlias(new GenericClass<string>("payload")) === "payload");
    acceptInnerAlias(undefined as any);

    const classUsingAlias = new ClassUsingAlias(10, "name");
    assert(classUsingAlias.id === 10);
    assert(classUsingAlias.name === "name");

    // --- Nested aliases (type-only usages via public namespaces) ---
    const nestedInt: NestedInt = 1;
    const nestedString: NestedString = "n";
    const nestedClassAlias: NestedClassAlias = new SomeClass("nested");
    const nestedGenericAlias: NestedGenericAlias<string> = new GenericClass<string>("ng");
    const nestedConcreteGenericAlias: NestedConcreteGenericAlias = new GenericClass<string>("nc");
    const nestedCallback: NestedCallback = () => {};
    const nestedNullable: NestedNullable = null;
    // `Self` aliases point back at their owner; exercise the constructable ones with real values.
    const classSelf: ClassSelf = new ClassWithNestedTypealiases();
    const genericSelf: GenericSelf<string> = new GenericClassWithNestedTypealiases<string>();
    const enumSelf: EnumSelf = EnumWithNestedTypealiases.Z;
    // Interface `Self` has no runtime constructor; use it in type positions (parameter + return).
    const identityInterfaceSelf = (self: InterfaceSelf): InterfaceSelf => self;
    // Object / companion-object `Self` aliases resolve to the singleton's own type (`typeof ...`).
    const objectSelf: ObjectSelf = ObjectWithNestedTypealiases;
    const companionSelf: CompanionSelf = ClassWithCompanionTypealiases.Companion;
    const namedCompanionSelf: NamedCompanionSelf = ClassWithNamedCompanionTypealiases.Named;
    let genericNestedGeneric: GenericNestedGenericAlias<string, number>;
    const interfaceNestedInt: InterfaceNestedInt = 2;
    let interfaceNestedClassAlias: InterfaceNestedClassAlias;
    let interfaceNestedGenericAlias: InterfaceNestedGenericAlias<string>;
    let interfaceNestedCallback: InterfaceNestedCallback;
    const enumNestedInt: EnumNestedInt = 3;
    let enumNestedClassAlias: EnumNestedClassAlias;
    const openClassNestedAlias: OpenClassNestedAlias = "open";
    const abstractNestedAlias: AbstractNestedAlias = 4;
    const outerNestedAlias: OuterNestedAlias = "outer";
    const deeplyNestedAlias: DeeplyNestedAlias = 5;
    let deeplyNestedClassAlias: DeeplyNestedClassAlias;
    const sealedNestedAlias: SealedNestedAlias = 6;
    const sealedInterfaceNestedAlias: SealedInterfaceNestedAlias = "sealed";
    const visibleAlias: VisibleAlias = 7;
    assert(nestedInt === 1);
    assert(nestedString === "n");
    assert(nestedClassAlias.value === "nested");
    assert(nestedGenericAlias.value === "ng");
    assert(nestedConcreteGenericAlias.value === "nc");
    nestedCallback();
    assert(nestedNullable === null);
    assert(classSelf instanceof ClassWithNestedTypealiases);
    assert(genericSelf instanceof GenericClassWithNestedTypealiases);
    assert(enumSelf.name === "Z");
    assert(typeof identityInterfaceSelf === "function");
    assert(objectSelf != null);
    assert(companionSelf != null);
    assert(namedCompanionSelf != null);
    assert(interfaceNestedInt === 2);
    assert(enumNestedInt === 3);
    assert(openClassNestedAlias === "open");
    assert(abstractNestedAlias === 4);
    assert(outerNestedAlias === "outer");
    assert(deeplyNestedAlias === 5);
    assert(sealedNestedAlias === 6);
    assert(sealedInterfaceNestedAlias === "sealed");
    assert(visibleAlias === 7);

    return "OK";
}
