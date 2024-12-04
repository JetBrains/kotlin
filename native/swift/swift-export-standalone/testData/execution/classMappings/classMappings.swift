import ClassMappings
import KotlinRuntime
import ObjectiveC

enum TestTypeError: Error {
    case classNotBound(Any.Type)
    case classNotFound(String)
    case classWrongType(actual: Any, expected: Any.Type)
}

func testType<T: KotlinBase>(_ optionalName: String?, _ type: T.Type) throws {
    guard let name = optionalName else { throw TestTypeError.classNotBound(type) }
    guard let objcClass = objc_getClass(name) else { throw TestTypeError.classNotFound(name) }
    guard let swiftClass = objcClass as? T.Type else { throw TestTypeError.classWrongType(actual: objcClass, expected: type) }
    try assertSame(actual: swiftClass, expected: type)
}

func testAnyClass() throws {
    try testType(getAnyClassName(), KotlinBase.self)
}

func testFinalClass() throws {
    try testType(getFinalClassName(), FinalClass.self)
}

func testNestedFinalClass() throws {
    try testType(getNestedFinalClassName(), FinalClass.NestedFinalClass.self)
}

func testNamespacedFinalClass() throws {
    try testType(namespace.getNamespacedFinalClassName(), namespace.NamespacedFinalClass.self)
}

func testOpenClass() throws {
    try testType(getOpenClassName(), OpenClass.self)
}

func testPrivateClass() throws {
    try assertNil(getPrivateClassName())
}

func testAbstractClass() throws {
    try testType(getAbstractClassName(), AbstractClass.self)
}

func testAbstractClassPrivateSubclass() throws {
    try assertNil(getAbstractClassPrivateSubclassName())
}

func testEnums() throws {
    try testType(getEnumClassName(), Enum.self)
}


class ClassMappingsTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testAnyClass", method: withAutorelease(testAnyClass)),
            TestCase(name: "testFinalClass", method: withAutorelease(testFinalClass)),
            TestCase(name: "testNestedFinalClass", method: withAutorelease(testNestedFinalClass)),
            TestCase(name: "testNamespacedFinalClass", method: withAutorelease(testNamespacedFinalClass)),
            TestCase(name: "testOpenClass", method: withAutorelease(testOpenClass)),
            TestCase(name: "testPrivateClass", method: withAutorelease(testPrivateClass)),
            TestCase(name: "testAbstractClass", method: withAutorelease(testAbstractClass)),
            TestCase(name: "testAbstractClassPrivateSubclass", method: withAutorelease(testAbstractClassPrivateSubclass)),
            TestCase(name: "testEnums", method: withAutorelease(testEnums)),
        ]
    }
}