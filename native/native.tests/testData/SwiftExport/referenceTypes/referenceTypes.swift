import ReferenceTypes

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
    try assertSame(actual: one, expected: two)
    try assertEquals(actual: getX(foo: one), expected: 1)
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
            TestCase(name: "objectGetter", method: withAutorelease(objectGetter)),
            TestCase(name: "objectSetter", method: withAutorelease(objectSetter)),
            TestCase(name: "objectMethod", method: withAutorelease(objectMethod)),
            TestCase(name: "primitiveGetterSetterInObject", method: withAutorelease(primitiveGetterSetterInObject)),
            TestCase(name: "primitiveMethodInObject", method: withAutorelease(primitiveMethodInObject)),
            TestCase(name: "objectGetterSetterInObject", method: withAutorelease(objectGetterSetterInObject)),
            TestCase(name: "objectMethodInObject", method: withAutorelease(objectMethodInObject)),
            TestCase(name: "multipleConstructors", method: withAutorelease(multipleConstructors)),
            TestCase(name: "typealiasPreservesIdentity", method: withAutorelease(typealiasPreservesIdentity)),
        ]
    }
}
