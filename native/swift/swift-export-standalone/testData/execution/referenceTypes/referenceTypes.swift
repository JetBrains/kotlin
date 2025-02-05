import ReferenceTypes
import second_main
import KotlinRuntime
import overrides
import overrides_across_modules

func initProducesNewObject() throws {
    let one = Foo(x: 1)
    let two = Foo(x: 2)
    try assertFalse(one === two)
}

func passInArgument() throws {
    let one = Foo(x: 1)
    try assertEquals(actual: getX(foo: one), expected: 1)
    let two = Foo(x: 2)
    try assertEquals(actual: getX(foo: two), expected: 2)
}

func getFromReturnValue() throws {
    let one = makeFoo(x: 1)
    let two = makeFoo(x: 2)
    try assertFalse(one === two)
    try assertEquals(actual: getX(foo: one), expected: 1)
    try assertEquals(actual: getX(foo: two), expected: 2)
}

func setGlobalVar() throws {
    let one = globalFoo
    globalFoo = Foo(x: getX(foo: one) + 1)
    let two = globalFoo
    try assertFalse(one === two)
    try assertEquals(actual: getX(foo: two), expected: getX(foo: one) + 1)
}

func objectIdentityWithGlobal() throws {
    let one = getGlobalFoo()
    let two = globalFoo
    let three = readGlobalFoo
    try assertSame(actual: one, expected: two)
    try assertSame(actual: one, expected: three)
}

func objectIdentityWithPassThrough() throws {
    let one = Foo(x: 1)
    let two = idFoo(foo: one)
    let three = extId(one)
    try assertSame(actual: one, expected: two)
    try assertSame(actual: one, expected: three)
    try assertEquals(actual: getX(foo: one), expected: 1)
    try assertEquals(actual: extGetX(one), expected: 1)
    try assertEquals(actual: getExtX(one), expected: 1)
    setExtX(one, v: 10)
    try assertEquals(actual: getExtX(one), expected: 10)
}

func objectIdentityWithObject() throws {
    let one = Baz.shared
    let two = Baz.shared
    let three = getBaz()
    try assertSame(actual: one, expected: two)
    try assertSame(actual: one, expected: three)
}

func noObjectIdentityWithGlobalPermanent() throws {
    let one = getGlobalPermanent()
    let two = getGlobalPermanent()
    try assertFalse(one === two)
    try assertEquals(actual: getPermanentId(permanent: one), expected: getPermanentId(permanent: two))
}

func noObjectIdentityWithPassThroughPermanent() throws {
    let one = getGlobalPermanent()
    let two = idPermanent(permanent: one)
    try assertFalse(one === two)
    try assertEquals(actual: getPermanentId(permanent: one), expected: getPermanentId(permanent: two))
}

func noObjectIdentityWithPermanent() throws {
    let one = Permanent.shared
    let two = Permanent.shared
    try assertFalse(one === two)
    try assertEquals(actual: getPermanentId(permanent: one), expected: getPermanentId(permanent: two))
}

func primitiveGetter() throws {
    let one = Foo(x: 1)
    try assertEquals(actual: one.x, expected: 1)
}

func primitiveSetter() throws {
    let one = Foo(x: 1)
    one.x = 2
    try assertEquals(actual: one.x, expected: 2)
}

func primitiveMethod() throws {
    let one = Foo(x: 1)
    let lastX = one.getAndSetX(newX: 2)
    try assertEquals(actual: lastX, expected: 1)
    try assertEquals(actual: one.x, expected: 2)
}

func memberExtension() throws {
    let foo = Foo(x: 1)
    let sum = foo.memberExt(10)
    try assertEquals(actual: sum, expected: 11)
}

func memberExtensionProperty() throws {
    let foo = Foo(x: 1)
    let sum = foo.getMemberExtProp(10)
    try assertEquals(actual: sum, expected: 11)
    foo.setMemberExtProp(10, v: 12)
    try assertEquals(actual: foo.x, expected: 2)
}

func objectGetter() throws {
    let one = Foo(x: 1)
    let two = Bar(foo: one)
    try assertSame(actual: two.foo, expected: one)
}

func objectSetter() throws {
    let one = Foo(x: 1)
    let two = Bar(foo: one)
    let three = Foo(x: 2)
    two.foo = three
    try assertSame(actual: two.foo, expected: three)
}

func objectMethod() throws {
    let one = Foo(x: 1)
    let two = Bar(foo: one)
    let three = Foo(x: 2)
    let lastFoo = two.getAndSetFoo(newFoo: three)
    try assertSame(actual: lastFoo, expected: one)
    try assertSame(actual: two.foo, expected: three)
}

func primitiveGetterSetterInObject() throws {
    Baz.shared.x = 1
    Baz.shared.x = 2
    try assertEquals(actual: Baz.shared.x, expected: 2)
}

func primitiveMethodInObject() throws {
    Baz.shared.x = 1
    let lastX = Baz.shared.getAndSetX(newX: 2)
    try assertEquals(actual: lastX, expected: 1)
    try assertEquals(actual: Baz.shared.x, expected: 2)
}

func objectGetterSetterInObject() throws {
    let one = Foo(x: 1)
    Baz.shared.foo = one
    let two = Foo(x: 2)
    Baz.shared.foo = two
    try assertSame(actual: Baz.shared.foo, expected: two)
}

func objectMethodInObject() throws {
    let one = Foo(x: 1)
    Baz.shared.foo = one
    let two = Foo(x: 2)
    let lastFoo = Baz.shared.getAndSetFoo(newFoo: two)
    try assertSame(actual: lastFoo, expected: one)
    try assertSame(actual: Baz.shared.foo, expected: two)
}

func multipleConstructors() throws {
    let one = Foo(x: 1)
    let two = Foo(f: 1.1)
    try assertEquals(actual: getX(foo: one), expected: getX(foo: two))
}

func typealiasPreservesIdentity() throws {
    let a = Foo(x: 1)
    let typealiased: FooAsTypealias = a
    try assertTrue(a === typealiased)
    try assertTrue(Foo.Type.self == FooAsTypealias.Type.self)
}

func objectsTravelBridgeAsAny() throws {
    let obj: KotlinBase = mainObject
    try assertTrue((obj as Any) is KotlinBase)
    try assertTrue(isMainObject(obj: obj))
}

func permanentObjectsTravelBridgeAsAny() throws {
    let obj: KotlinBase = getMainPermanentObject()
    try assertTrue(isMainPermanentObject(obj: obj))
    try assertFalse(isMainPermanentObject(obj: mainObject))

    let fieldObj = Object.shared.instance
    try assertTrue(Object.shared.isInstance(obj: fieldObj))
}

func anyPersistsAsProperty() throws {
    let bar = SomeBar()
    let baz = SomeBaz()
    let foo = SomeFoo(storage: bar)

    try assertTrue(foo.storage === bar)
    foo.storage = baz
    try assertTrue(foo.storage === baz)
}

func depsObjectsTravelBridgeAsAny() throws {
    let obj: KotlinBase = deps_instance
    try assertTrue((obj as Any) is KotlinBase)
    try assertTrue(isDepsObject(obj: obj))
    try assertTrue(isSavedDepsObject(obj: obj))
}

func depsObjectsTravelBridgeAsAny2() throws {
    let obj: KotlinBase = deps_instance_2
    try assertTrue((obj as Any) is KotlinBase)
    try assertTrue(isDepsObject_2(obj: obj))
    try assertTrue(isSavedDepsObject_2(obj: obj))
}

func classWithFactory() throws {
    try assertEquals(actual: classWithFactory(longValue: 42).value, expected: 42)
    try assertEquals(actual: ClassWithFactory(value: 11).value, expected: 11)
}

func objectsHashProperly() throws {
    let one: KotlinBase = getHashableObject(value: 1)
    let ein: KotlinBase = getHashableObject(value: 1)
    let two: KotlinBase = getHashableObject(value: 2)

    try assertFalse(one === ein)
    try assertTrue(one == ein)
    try assertFalse(one == two)
    try assertFalse(ein == two)

    // NSNumber isn't a `KotlinObject`, but conforms to our `toKotlin:` informal protocol on `NSObject`
    try assertTrue(ein == NSNumber(value: CInt(1)))
    try assertFalse(ein == NSNumber(value: CInt(2)))

    // `CInt` is bridged as `NSNumber`
    // We are using `isEqual(_:)` here to trigger objc bridging
    try assertTrue(ein.isEqual(CInt(1)))
    try assertFalse(ein.isEqual(CInt(2)))

    // On apple platforms, swift classes with no objc inheritance implicitly inherit
    // `Swift._SwiftObject` – a separate from `NSObject` root class that we expect to
    // also conform to our informal `toKotlin:` protocol
    class MyRoot {}
    try assertFalse(ein.isEqual(MyRoot()))

    func testEquality(_ lhs: KotlinBase, _ rhs: KotlinBase) throws {
        try assertEquals(actual: lhs.hashValue, expected: numericCast(getHash(obj: lhs)))
        try assertEquals(actual: lhs == lhs, expected: true)
        try assertEquals(actual: rhs.hashValue, expected: numericCast(getHash(obj: rhs)))
        try assertEquals(actual: rhs == rhs, expected: true)

        try assertEquals(actual: lhs == rhs, expected: isEqual(lhs: lhs, rhs: rhs))
        try assertEquals(actual: lhs.hashValue == rhs.hashValue, expected: getHash(obj: lhs) == getHash(obj: rhs))
    }

    try testEquality(one, one)
    try testEquality(two, two)
    try testEquality(ein, ein)

    try testEquality(one, ein)
    try testEquality(one, two)
    try testEquality(ein, two)
}

func openClasses() throws {
    let base = Base()
    let derived = Derived()

    try assertSame(actual: identity(obj: base), expected: base)
    try assertSame(actual: identity(obj: derived), expected: derived)

    try assertFalse(base === derived)
    try assertEquals(actual: ObjectIdentifier(type(of: base)), expected: ObjectIdentifier(Base.self))
    try assertEquals(actual: ObjectIdentifier(type(of: derived)), expected: ObjectIdentifier(Derived.self))

    try assertEquals(actual: base.test(), expected: 42)
    try assertEquals(actual: derived.test(), expected: 42)
}

func openClassesAdhereToLSP() throws {
    let base: Base = getBase()
    try assertTrue(type(of: base) == Base.self)

    let derived: Base = getDerived()
    try assertTrue(type(of: derived) == Derived.self)

    try assertTrue(type(of: polymorphicObject) == Derived.self)
    try assertTrue(polymorphicObject !== base)
    polymorphicObject = base
    try assertSame(actual: polymorphicObject, expected: base)
    polymorphicObject = derived
    try assertSame(actual: polymorphicObject, expected: derived)

    let privateImpl = getPrivateImpl()
    let impl = getImpl()

    try assertTrue(type(of: privateImpl) == Abstract.self)
    try assertTrue(type(of: impl) == Impl.self)

    try assertTrue(type(of: abstractPolymorphicObject) == Impl.self)
    try assertTrue(abstractPolymorphicObject !== impl)
    try assertTrue(abstractPolymorphicObject !== privateImpl)

    abstractPolymorphicObject = impl
    try assertSame(actual: abstractPolymorphicObject, expected: impl)
    try assertTrue(abstractPolymorphicObject !== privateImpl)

    abstractPolymorphicObject = privateImpl
    try assertTrue(abstractPolymorphicObject !== impl)
    try assertSame(actual: abstractPolymorphicObject, expected: privateImpl)
}

func companionObject() throws {
    try assertTrue(HostBase.Companion.shared != HostDerived.Companion.shared)
    try assertEquals(actual: HostBase.Companion.shared.hostDepth, expected: 0)
    try assertEquals(actual: HostDerived.Companion.shared.hostDepth, expected: 1)
}

func overridesShouldWork() throws {
    let parent: Parent = Parent(value: "10")
    let child: Parent = Child(value: 20)
    let grandchild: Parent = GrandChild(value: Int16(30))

    try assertEquals(actual: parent.value, expected: "10")
    try assertEquals(actual: child.value, expected: "20")
    try assertEquals(actual: grandchild.value, expected: "30")

    try assertEquals(actual: parent.foo(), expected: "Parent")
    try assertEquals(actual: child.foo(), expected: "Child")
    try assertEquals(actual: grandchild.foo(), expected: "GrandChild")

    parent.bar += 5
    try assertEquals(actual: parent.bar, expected: 15)

    child.bar += 5
    try assertEquals(actual: child.bar, expected: 25)

    try assertEquals(actual: grandchild.bar, expected: 42)
    grandchild.bar = 50
    try assertEquals(actual: grandchild.bar, expected: 42)

    try assertEquals(actual: parent.hop(), expected: "Parent")
    try assertEquals(actual: child.hop(), expected: "Parent")
    try assertEquals(actual: grandchild.hop(), expected: "GrandChild")

    try assertEquals(actual: parent.chain(), expected: "Parent")
    try assertEquals(actual: child.chain(), expected: "Child")
    try assertEquals(actual: grandchild.chain(), expected: "GrandChild")

    try assertEquals(actual: parent.poly(), expected: parent)
    try assertEquals(actual: child.poly(), expected: child)
    try assertEquals(actual: grandchild.poly(), expected: grandchild)

    try assertEquals(actual: parent.nullable(), expected: parent)
    try assertEquals(actual: child.nullable(), expected: child)
    try assertEquals(actual: grandchild.nullable(), expected: grandchild)

    let abstractParentImpl1 = AbstractParentImpl()
    let abstractParentImpl2: AbstractParent = AbstractParentImpl()
    let abstractParentImpl3 = getAbstractParentImpl()
    let abstractParentPrivateImpl = getAbstractParentPrivateImpl()

    try assertEquals(actual: abstractParentImpl1.foo(), expected: "AbstractParentImpl")
    try assertEquals(actual: abstractParentImpl2.foo(), expected: "AbstractParentImpl")
    try assertEquals(actual: abstractParentImpl3.foo(), expected: "AbstractParentImpl")
    try assertEquals(actual: abstractParentPrivateImpl.foo(), expected: "AbstractParentPrivateImpl")
}

func overridesShouldWorkAcrossModules() throws {
    let parent: Parent = Parent(value: "parent")
    let cousin: Parent = Cousin(value: "cousin")

    try assertEquals(actual: parent.value, expected: "parent")
    try assertEquals(actual: cousin.value, expected: "cousin")

    try assertEquals(actual: parent.foo(), expected: "Parent")
    try assertEquals(actual: cousin.foo(), expected: "Cousin")

    parent.bar += 5
    try assertEquals(actual: parent.bar, expected: 15)

    cousin.bar += 5
    try assertEquals(actual: cousin.bar, expected: 26)

    try assertEquals(actual: parent.hop(), expected: "Parent")
    try assertEquals(actual: cousin.hop(), expected: "Parent")

    try assertEquals(actual: parent.chain(), expected: "Parent")
    try assertEquals(actual: cousin.chain(), expected: "Cousin")

    try assertEquals(actual: parent.poly(), expected: parent)
    try assertEquals(actual: cousin.poly(), expected: cousin)

    try assertEquals(actual: parent.nullable(), expected: parent)
    try assertEquals(actual: cousin.nullable(), expected: cousin)
}

func dataClassesShouldWork() throws {
    let one = DataClass(i: 1, s: "a")
    let two = DataClass(i: 2, s: "b")
    let oneCopy = one.copy(i: 2, s: "b")

    try assertEquals(actual: one == two, expected: false)
    try assertEquals(actual: two == oneCopy, expected: true)

    try assertEquals(actual: "\(one)", expected: "DataClass(i=1, s=a)")
    try assertEquals(actual: one.hashValue, expected: 128)
}


func testEnums() throws {
    let en = Enum.a
    try assertEquals(actual: en.print(), expected: "1 - str")
    en.i = 3
    try assertEquals(actual: en.print(), expected: "3 - str")
    try assertEquals(actual: Enum.a.print(), expected: "3 - str")

    try assertEquals(actual: Enum.b.print(), expected: "rts - 5")
    try assertEquals(actual: Enum.valueOf(value: "b").print(), expected: "rts - 5")

    switch en {
    case .a: break;
    default: try fail("switch over kotlin enum class should work")
    }

    try assertEquals(actual: Enum.allCases, expected: [Enum.a, Enum.b])
}

class ReferenceTypesTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "initProducesNewObject", method: withAutorelease(initProducesNewObject)),
            TestCase(name: "passInArgument", method: withAutorelease(passInArgument)),
            TestCase(name: "getFromReturnValue", method: withAutorelease(getFromReturnValue)),
            TestCase(name: "setGlobalVar", method: withAutorelease(setGlobalVar)),
            TestCase(name: "objectIdentityWithGlobal", method: withAutorelease(objectIdentityWithGlobal)),
            TestCase(name: "objectIdentityWithPassThrough", method: withAutorelease(objectIdentityWithPassThrough)),
            TestCase(name: "objectIdentityWithObject", method: withAutorelease(objectIdentityWithObject)),
            TestCase(name: "noObjectIdentityWithGlobalPermanent", method: withAutorelease(noObjectIdentityWithGlobalPermanent)),
            TestCase(name: "noObjectIdentityWithPassThroughPermanent", method: withAutorelease(noObjectIdentityWithPassThroughPermanent)),
            TestCase(name: "noObjectIdentityWithPermanent", method: withAutorelease(noObjectIdentityWithPermanent)),
            TestCase(name: "primitiveGetter", method: withAutorelease(primitiveGetter)),
            TestCase(name: "primitiveSetter", method: withAutorelease(primitiveSetter)),
            TestCase(name: "primitiveMethod", method: withAutorelease(primitiveMethod)),
            TestCase(name: "memberExtension", method: withAutorelease(memberExtension)),
            TestCase(name: "memberExtensionProperty", method: withAutorelease(memberExtensionProperty)),
            TestCase(name: "objectGetter", method: withAutorelease(objectGetter)),
            TestCase(name: "objectSetter", method: withAutorelease(objectSetter)),
            TestCase(name: "objectMethod", method: withAutorelease(objectMethod)),
            TestCase(name: "primitiveGetterSetterInObject", method: withAutorelease(primitiveGetterSetterInObject)),
            TestCase(name: "primitiveMethodInObject", method: withAutorelease(primitiveMethodInObject)),
            TestCase(name: "objectGetterSetterInObject", method: withAutorelease(objectGetterSetterInObject)),
            TestCase(name: "objectMethodInObject", method: withAutorelease(objectMethodInObject)),
            TestCase(name: "multipleConstructors", method: withAutorelease(multipleConstructors)),
            TestCase(name: "typealiasPreservesIdentity", method: withAutorelease(typealiasPreservesIdentity)),
            TestCase(name: "objectsTravelBridgeAsAny", method: withAutorelease(objectsTravelBridgeAsAny)),
            TestCase(name: "permanentObjectsTravelBridgeAsAny", method: withAutorelease(permanentObjectsTravelBridgeAsAny)),
            TestCase(name: "anyPersistsAsProperty", method: withAutorelease(anyPersistsAsProperty)),
            TestCase(name: "depsObjectsTravelBridgeAsAny", method: withAutorelease(depsObjectsTravelBridgeAsAny)),
            TestCase(name: "depsObjectsTravelBridgeAsAny2", method: withAutorelease(depsObjectsTravelBridgeAsAny2)),
            TestCase(name: "classWithFactory", method: withAutorelease(classWithFactory)),
            TestCase(name: "objectsHashProperly", method: withAutorelease(objectsHashProperly)),
            TestCase(name: "openClasses", method: withAutorelease(openClasses)),
            TestCase(name: "openClassesAdhereToLSP", method: withAutorelease(openClassesAdhereToLSP)),
            TestCase(name: "companionObject", method: withAutorelease(companionObject)),
            TestCase(name: "overridesShouldWork", method: withAutorelease(overridesShouldWork)),
            TestCase(name: "overridesShouldWorkAcrossModules", method: withAutorelease(overridesShouldWorkAcrossModules)),
            TestCase(name: "dataClassesShouldWork", method: withAutorelease(dataClassesShouldWork)),
            TestCase(name: "testEnums", method: withAutorelease(testEnums)),
        ]
    }
}
