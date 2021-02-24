"use strict";
var ClassWithDefaultCtor = JS_TESTS.ClassWithDefaultCtor, ClassWithPrimaryCtor = JS_TESTS.ClassWithPrimaryCtor, ClassWithSecondaryCtor = JS_TESTS.ClassWithSecondaryCtor, ClassWithMultipleSecondaryCtors = JS_TESTS.ClassWithMultipleSecondaryCtors, DerivedClassWithSecondaryCtor = JS_TESTS.DerivedClassWithSecondaryCtor, OpenClassWithMixedConstructors = JS_TESTS.OpenClassWithMixedConstructors, KotlinGreeter = JS_TESTS.KotlinGreeter;
function box() {
    var o1 = new ClassWithDefaultCtor();
    if (o1.x !== "ClassWithDefaultCtor::x")
        return "Fail: ClassWithDefaultCtor";
    var o2 = new ClassWithPrimaryCtor("foo");
    if (o2.x !== "foo")
        return "Fail: ClassWithPrimaryCtor";
    var o3 = ClassWithSecondaryCtor.create("foo2");
    if (o3.x !== "foo2")
        return "Fail: ClassWithSecondaryCtor.create";
    var o4 = ClassWithMultipleSecondaryCtors.createFromString("foo3");
    if (o4.x !== "fromString:foo3")
        return "Fail: ClassWithMultipleSecondaryCtors.createFromString";
    var o5 = ClassWithMultipleSecondaryCtors.createFromInts(1, 2);
    if (o5.x !== "fromInts:1:2")
        return "Fail: ClassWithMultipleSecondaryCtors.createFromInts";
    var o6 = new OpenClassWithMixedConstructors("foo4");
    if (o6.x !== "foo4")
        return "Fail: OpenClassWithMixedConstructors";
    var o7 = OpenClassWithMixedConstructors.createFromStrings("foo", "bar");
    if (o7.x !== "fromStrings:foo:bar")
        return "Fail: OpenClassWithMixedConstructors.createFromStrings";
    var o8 = OpenClassWithMixedConstructors.createFromInts(10, -20);
    if (o8.x !== "fromStrings:10:-20")
        return "Fail: OpenClassWithMixedConstructors.createFromInts";
    var o9 = DerivedClassWithSecondaryCtor.delegateToPrimary("foo6");
    if (o9.x !== "foo6")
        return "Fail: DerivedClassWithSecondaryCtor.delegateToPrimary";
    var o10 = DerivedClassWithSecondaryCtor.delegateToCreateFromInts(-10, 20);
    if (o10.x !== "fromStrings:-10:20")
        return "Fail: DerivedClassWithSecondaryCtor.delegateToCreateFromInts";
    var kg = new KotlinGreeter("Hi");
    if (kg.greeting != "Hi")
        return "Fail: KotlinGreeter";
    return "OK";
}
