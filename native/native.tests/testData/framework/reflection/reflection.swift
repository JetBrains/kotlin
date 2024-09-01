import Reflection

class MyClass : MyOpenClass {}

func testReflection() throws {
    let result = ReflectionKt.checkMyOpenClass(instance: MyClass())
    try assertEquals(actual: result, expected: "OK")
}

// -------- Execution of the test --------

class ReflectionTests : SimpleTestProvider {
    override init() {
        super.init()

        test("reflection", testReflection)
    }
}
