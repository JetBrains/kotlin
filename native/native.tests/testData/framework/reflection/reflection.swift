import Reflection

class MyClass : MyOpenClass {}

func testMyClassReflection() throws {
    let result = ReflectionKt.checkMyClass(instance: MyClass())
    try assertEquals(actual: result, expected: "OK")
}

func testMyOpenClassObjCName() throws {
    let name = ReflectionKt.getMyOpenClassObjCName()
    try assertEquals(actual: name, expected: "ReflectionMyOpenClass")
}

// -------- Execution of the test --------

class ReflectionTests : SimpleTestProvider {
    override init() {
        super.init()

        test("testMyClassReflection", testMyClassReflection)
        test("testMyOpenClassObjCName", testMyOpenClassObjCName)
    }
}
