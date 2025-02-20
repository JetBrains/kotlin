import ReferenceTypes
import second_main
import KotlinRuntime
import overrides
import overrides_across_modules
import Testing

@Test
func initProducesNewObject() throws {
    let one = Foo(x: 1)
    let two = Foo(x: 2)
    try #require(one !== two)
}

@Test
func passInArgument() throws {
    let one = Foo(x: 1)
    try #require(getX(foo: one) == 1)
    let two = Foo(x: 2)
    try #require(getX(foo: two) == 2)
}

@Test
func getFromReturnValue() throws {
    let one = makeFoo(x: 1)
    let two = makeFoo(x: 2)
    try #require(one !== two)
    try #require(getX(foo: one) == 1)
    try #require(getX(foo: two) == 2)
}

@Test
func setGlobalVar() throws {
    let one = globalFoo
    globalFoo = Foo(x: getX(foo: one) + 1)
    let two = globalFoo
    try #require(one !== two)
    try #require(getX(foo: two) == getX(foo: one) + 1)
}

@Test
func objectIdentityWithGlobal() throws {
    let one = getGlobalFoo()
    let two = globalFoo
    let three = readGlobalFoo
    try #require(one === two)
    try #require(one === three)
}

@Test
func objectIdentityWithPassThrough() throws {
    let one = Foo(x: 1)
    let two = idFoo(foo: one)
    let three = extId(one)
    try #require(one === two)
    try #require(one === three)
    try #require(getX(foo: one) == 1)
    try #require(extGetX(one) == 1)
    try #require(getExtX(one) == 1)
    setExtX(one, v: 10)
    try #require(getExtX(one) == 10)
}

@Test
func objectIdentityWithObject() throws {
    let one = Baz.shared
    let two = Baz.shared
    let three = getBaz()
    try #require(one === two)
    try #require(one === three)
}

@Test
func noObjectIdentityWithGlobalPermanent() throws {
    let one = getGlobalPermanent()
    let two = getGlobalPermanent()
    try #require(one !== two)
    try #require(getPermanentId(permanent: one) == getPermanentId(permanent: two))
}

@Test
func noObjectIdentityWithPassThroughPermanent() throws {
    let one = getGlobalPermanent()
    let two = idPermanent(permanent: one)
    try #require(one !== two)
    try #require(getPermanentId(permanent: one) == getPermanentId(permanent: two))
}

@Test
func noObjectIdentityWithPermanent() throws {
    let one = Permanent.shared
    let two = Permanent.shared
    try #require(one !== two)
    try #require(getPermanentId(permanent: one) == getPermanentId(permanent: two))
}

@Test
func primitiveGetter() throws {
    let one = Foo(x: 1)
    try #require(one.x == 1)
}

@Test
func primitiveSetter() throws {
    let one = Foo(x: 1)
    one.x = 2
    try #require(one.x == 2)
}

@Test
func primitiveMethod() throws {
    let one = Foo(x: 1)
    let lastX = one.getAndSetX(newX: 2)
    try #require(lastX == 1)
    try #require(one.x == 2)
}

@Test
func memberExtension() throws {
    let foo = Foo(x: 1)
    let sum = foo.memberExt(10)
    try #require(sum == 11)
}

@Test
func memberExtensionProperty() throws {
    let foo = Foo(x: 1)
    let sum = foo.getMemberExtProp(10)
    try #require(sum == 11)
    foo.setMemberExtProp(10, v: 12)
    try #require(foo.x == 2)
}

func objectGetter() throws {
    let one = Foo(x: 1)
    let two = Bar(foo: one)
    try #require(two.foo === one)
}

@Test
func objectSetter() throws {
    let one = Foo(x: 1)
    let two = Bar(foo: one)
    let three = Foo(x: 2)
    two.foo = three
    try #require(two.foo === three)
}

@Test
func objectMethod() throws {
    let one = Foo(x: 1)
    let two = Bar(foo: one)
    let three = Foo(x: 2)
    let lastFoo = two.getAndSetFoo(newFoo: three)
    try #require(lastFoo === one)
    try #require(two.foo === three)
}

@Test
func primitiveGetterSetterInObject() throws {
    Baz.shared.x = 1
    Baz.shared.x = 2
    try #require(Baz.shared.x == 2)
}

@Test
func primitiveMethodInObject() throws {
    Baz.shared.x = 1
    let lastX = Baz.shared.getAndSetX(newX: 2)
    try #require(lastX == 1)
    try #require(Baz.shared.x == 2)
}

@Test
func objectGetterSetterInObject() throws {
    let one = Foo(x: 1)
    Baz.shared.foo = one
    let two = Foo(x: 2)
    Baz.shared.foo = two
    try #require(Baz.shared.foo === two)
}

@Test
func objectMethodInObject() throws {
    let one = Foo(x: 1)
    Baz.shared.foo = one
    let two = Foo(x: 2)
    let lastFoo = Baz.shared.getAndSetFoo(newFoo: two)
    try #require(lastFoo === one)
    try #require(Baz.shared.foo === two)
}

@Test
func multipleConstructors() throws {
    let one = Foo(x: 1)
    let two = Foo(f: 1.1)
    try #require(getX(foo: one) == getX(foo: two))
}

@Test
func typealiasPreservesIdentity() throws {
    let a = Foo(x: 1)
    let typealiased: FooAsTypealias = a
    try #require(a === typealiased)
    try #require(Foo.Type.self == FooAsTypealias.Type.self)
}

@Test
func objectsTravelBridgeAsAny() throws {
    let obj: KotlinBase = mainObject
    try #require((obj as Any) is KotlinBase)
    try #require(isMainObject(obj: obj))
}

@Test
func permanentObjectsTravelBridgeAsAny() throws {
    let obj: KotlinBase = getMainPermanentObject()
    try #require(isMainPermanentObject(obj: obj))
    try #require(!isMainPermanentObject(obj: mainObject))

    let fieldObj = Object.shared.instance
    try #require(Object.shared.isInstance(obj: fieldObj))
}

@Test
func anyPersistsAsProperty() throws {
    let bar = SomeBar()
    let baz = SomeBaz()
    let foo = SomeFoo(storage: bar)

    try #require(foo.storage === bar)
    foo.storage = baz
    try #require(foo.storage === baz)
}

@Test
func depsObjectsTravelBridgeAsAny() throws {
    let obj: KotlinBase = deps_instance
    try #require((obj as Any) is KotlinBase)
    try #require(isDepsObject(obj: obj))
    try #require(isSavedDepsObject(obj: obj))
}

@Test
func depsObjectsTravelBridgeAsAny2() throws {
    let obj: KotlinBase = deps_instance_2
    try #require((obj as Any) is KotlinBase)
    try #require(isDepsObject_2(obj: obj))
    try #require(isSavedDepsObject_2(obj: obj))
}

@Test
func classWithFactory() throws {
    try #require(classWithFactory(longValue: 42).value == 42)
    try #require(ClassWithFactory(value: 11).value == 11)
}

@Test
func objectsHashProperly() throws {
    let one: KotlinBase = getHashableObject(value: 1)
    let ein: KotlinBase = getHashableObject(value: 1)
    let two: KotlinBase = getHashableObject(value: 2)

    try #require(one !== ein)
    try #require(one == ein)
    try #require(one != two)
    try #require(ein != two)

    // NSNumber isn't a `KotlinObject`, but conforms to our `toKotlin:` informal protocol on `NSObject`
    try #require(ein == NSNumber(value: CInt(1)))
    try #require(ein != NSNumber(value: CInt(2)))

    // `CInt` is bridged as `NSNumber`
    // We are using `isEqual(_:)` here to trigger objc bridging
    try #require(ein.isEqual(CInt(1)))
    try #require(!ein.isEqual(CInt(2)))

    // On apple platforms, swift classes with no objc inheritance implicitly inherit
    // `Swift._SwiftObject` – a separate from `NSObject` root class that we expect to
    // also conform to our informal `toKotlin:` protocol
    class MyRoot {}
    try #require(!ein.isEqual(MyRoot()))

    func testEquality(_ lhs: KotlinBase, _ rhs: KotlinBase) throws {
        try #require(lhs.hashValue == numericCast(getHash(obj: lhs)))
        try #require(lhs == lhs)
        try #require(rhs.hashValue == numericCast(getHash(obj: rhs)))
        try #require(rhs == rhs)

        try #require((lhs == rhs) == (isEqual(lhs: lhs, rhs: rhs)))
        try #require((lhs.hashValue == rhs.hashValue) == (getHash(obj: lhs) == getHash(obj: rhs)))
    }

    try testEquality(one, one)
    try testEquality(two, two)
    try testEquality(ein, ein)

    try testEquality(one, ein)
    try testEquality(one, two)
    try testEquality(ein, two)
}

@Test
func openClasses() throws {
    let base = Base()
    let derived = Derived()

    try #require(identity(obj: base) === base)
    try #require(identity(obj: derived) === derived)

    try #require(base !== derived)
    try #require(ObjectIdentifier(type(of: base)) == ObjectIdentifier(Base.self))
    try #require(ObjectIdentifier(type(of: derived)) == ObjectIdentifier(Derived.self))

    try #require(base.test() == 42)
    try #require(derived.test() == 42)
}

@Test
func openClassesAdhereToLSP() throws {
    let base: Base = getBase()
    try #require(type(of: base) == Base.self)

    let derived: Base = getDerived()
    try #require(type(of: derived) == Derived.self)

    try #require(type(of: polymorphicObject) == Derived.self)
    try #require(polymorphicObject !== base)
    polymorphicObject = base
    try #require(polymorphicObject === base)
    polymorphicObject = derived
    try #require(polymorphicObject === derived)

    let privateImpl = getPrivateImpl()
    let impl = getImpl()

    try #require(type(of: privateImpl) == Abstract.self)
    try #require(type(of: impl) == Impl.self)

    try #require(type(of: abstractPolymorphicObject) == Impl.self)
    try #require(abstractPolymorphicObject !== impl)
    try #require(abstractPolymorphicObject !== privateImpl)

    abstractPolymorphicObject = impl
    try #require(abstractPolymorphicObject === impl)
    try #require(abstractPolymorphicObject !== privateImpl)

    abstractPolymorphicObject = privateImpl
    try #require(abstractPolymorphicObject !== impl)
    try #require(abstractPolymorphicObject === privateImpl)
}

@Test
func companionObject() throws {
    try #require(HostBase.Companion.shared != HostDerived.Companion.shared)
    try #require(HostBase.Companion.shared.hostDepth == 0)
    try #require(HostDerived.Companion.shared.hostDepth == 1)
}

@Test
func overridesShouldWork() throws {
    let parent: Parent = Parent(value: "10")
    let child: Parent = Child(value: 20)
    let grandchild: Parent = GrandChild(value: Int16(30))

    try #require(parent.value == "10")
    try #require(child.value == "20")
    try #require(grandchild.value == "30")

    try #require(parent.foo() == "Parent")
    try #require(child.foo() == "Child")
    try #require(grandchild.foo() == "GrandChild")

    parent.bar += 5
    try #require(parent.bar == 15)

    child.bar += 5
    try #require(child.bar == 25)

    try #require(grandchild.bar == 42)
    grandchild.bar = 50
    try #require(grandchild.bar == 42)

    try #require(parent.hop() == "Parent")
    try #require(child.hop() == "Parent")
    try #require(grandchild.hop() == "GrandChild")

    try #require(parent.chain() == "Parent")
    try #require(child.chain() == "Child")
    try #require(grandchild.chain() == "GrandChild")

    try #require(parent.poly() == parent)
    try #require(child.poly() == child)
    try #require(grandchild.poly() == grandchild)

    try #require(parent.nullable() == parent)
    try #require(child.nullable() == child)
    try #require(grandchild.nullable() == grandchild)

    let abstractParentImpl1 = AbstractParentImpl()
    let abstractParentImpl2: AbstractParent = AbstractParentImpl()
    let abstractParentImpl3 = getAbstractParentImpl()
    let abstractParentPrivateImpl = getAbstractParentPrivateImpl()

    try #require(abstractParentImpl1.foo() == "AbstractParentImpl")
    try #require(abstractParentImpl2.foo() == "AbstractParentImpl")
    try #require(abstractParentImpl3.foo() == "AbstractParentImpl")
    try #require(abstractParentPrivateImpl.foo() == "AbstractParentPrivateImpl")
}

@Test
func overridesShouldWorkAcrossModules() throws {
    let parent: Parent = Parent(value: "parent")
    let cousin: Parent = Cousin(value: "cousin")

    try #require(parent.value == "parent")
    try #require(cousin.value == "cousin")

    try #require(parent.foo() == "Parent")
    try #require(cousin.foo() == "Cousin")

    parent.bar += 5
    try #require(parent.bar == 15)

    cousin.bar += 5
    try #require(cousin.bar == 26)

    try #require(parent.hop() == "Parent")
    try #require(cousin.hop() == "Parent")

    try #require(parent.chain() == "Parent")
    try #require(cousin.chain() == "Cousin")

    try #require(parent.poly() == parent)
    try #require(cousin.poly() == cousin)

    try #require(parent.nullable() == parent)
    try #require(cousin.nullable() == cousin)
}

@Test
func dataClassesShouldWork() throws {
    let one = DataClass(i: 1, s: "a")
    let two = DataClass(i: 2, s: "b")
    let oneCopy = one.copy(i: 2, s: "b")

    try #require(one != two)
    try #require(two == oneCopy)

    try #require("\(one)" == "DataClass(i=1, s=a)")
    try #require(one.hashValue == 128)
}

@Test
func testEnums() throws {
    let en = Enum.a
    try #require(en.print() == "1 - str")
    en.i = 3
    try #require(en.print() == "3 - str")
    try #require(Enum.a.print() == "3 - str")

    try #require(Enum.b.print() == "rts - 5")
    try #require(Enum.valueOf(value: "b").print() == "rts - 5")

    switch en {
    case .a: break;
    default: try #require(Bool(false), "switch over kotlin enum class should work")
    }

    try #require(Enum.allCases == [Enum.a, Enum.b])
}
