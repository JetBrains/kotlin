import ClassMappings
import KotlinRuntime
import ObjectiveC
import Testing

enum TestTypeError: Error {
    case classNotBound(Any.Type)
    case classNotFound(String)
    case classWrongType(actual: Any, expected: Any.Type)
}

func testType<T: KotlinBase>(_ optionalName: String?, _ type: T.Type) throws {
    guard let name = optionalName else { throw TestTypeError.classNotBound(type) }
    guard let objcClass = objc_getClass(name) else { throw TestTypeError.classNotFound(name) }
    guard let swiftClass = objcClass as? T.Type else { throw TestTypeError.classWrongType(actual: objcClass, expected: type) }
    try #require(swiftClass === type)
}

@Test
func testAnyClass() throws {
    try testType(getAnyClassName(), KotlinBase.self)
}

@Test
func testFinalClass() throws {
    try testType(getFinalClassName(), FinalClass.self)
}

@Test
func testNestedFinalClass() throws {
    try testType(getNestedFinalClassName(), FinalClass.NestedFinalClass.self)
}

@Test
func testNamespacedFinalClass() throws {
    try testType(namespace.getNamespacedFinalClassName(), namespace.NamespacedFinalClass.self)
}

@Test
func testOpenClass() throws {
    try testType(getOpenClassName(), OpenClass.self)
}

@Test
func testPrivateClass() throws {
    try #require(getPrivateClassName() == nil)
}

@Test
func testAbstractClass() throws {
    try testType(getAbstractClassName(), AbstractClass.self)
}

@Test
func testAbstractClassPrivateSubclass() throws {
    try #require(getAbstractClassPrivateSubclassName() == nil)
}

@Test
func testEnums() throws {
    try testType(getEnumClassName(), Enum.self)
}