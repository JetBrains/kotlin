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
            TestCase(name: "noObjectIdentityWithGlobalPermanent", method: withAutorelease(noObjectIdentityWithGlobalPermanent)),
            TestCase(name: "noObjectIdentityWithPassThroughPermanent", method: withAutorelease(noObjectIdentityWithPassThroughPermanent)),
        ]
    }
}