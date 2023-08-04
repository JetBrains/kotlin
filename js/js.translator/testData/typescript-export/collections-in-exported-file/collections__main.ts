const {
    ClassWithDefaultCtor,
    ClassWithPrimaryCtor,
    ClassWithSecondaryCtor,
    ClassWithMultipleSecondaryCtors,
    DerivedClassWithSecondaryCtor,
    OpenClassWithMixedConstructors,
    KotlinGreeter
} = JS_TESTS;

function box(): string {
    const o1 = new ClassWithDefaultCtor();
    if (o1.x !== "ClassWithDefaultCtor::x") return "Fail: ClassWithDefaultCtor";


    const o2 = new ClassWithPrimaryCtor("foo");
    if (o2.x !== "foo") return "Fail: ClassWithPrimaryCtor";


    const o3 = ClassWithSecondaryCtor.create("foo2");
    if (o3.x !== "foo2") return "Fail: ClassWithSecondaryCtor.create";


    const o4 = ClassWithMultipleSecondaryCtors.createFromString("foo3");
    if (o4.x !== "fromString:foo3") return "Fail: ClassWithMultipleSecondaryCtors.createFromString";

    const o5 = ClassWithMultipleSecondaryCtors.createFromInts(1, 2);
    if (o5.x !== "fromInts:1:2") return "Fail: ClassWithMultipleSecondaryCtors.createFromInts";


    const o6 = new OpenClassWithMixedConstructors("foo4");
    if (o6.x !== "foo4") return "Fail: OpenClassWithMixedConstructors";

    const o7 = OpenClassWithMixedConstructors.createFromStrings("foo", "bar");
    if (o7.x !== "fromStrings:foo:bar") return "Fail: OpenClassWithMixedConstructors.createFromStrings";

    const o8 = OpenClassWithMixedConstructors.createFromInts(10, -20);
    if (o8.x !== "fromStrings:10:-20") return "Fail: OpenClassWithMixedConstructors.createFromInts";


    const o9 = DerivedClassWithSecondaryCtor.delegateToPrimary("foo6");
    if (o9.x !== "foo6") return "Fail: DerivedClassWithSecondaryCtor.delegateToPrimary";

    const o10 = DerivedClassWithSecondaryCtor.delegateToCreateFromInts(-10, 20);
    if (o10.x !== "fromStrings:-10:20") return "Fail: DerivedClassWithSecondaryCtor.delegateToCreateFromInts";


    const kg = new KotlinGreeter("Hi");
    if (kg.greeting != "Hi") return "Fail: KotlinGreeter";

    return "OK";
}