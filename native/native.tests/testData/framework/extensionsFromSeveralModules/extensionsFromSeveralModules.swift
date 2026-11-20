import ExtensionsFromSeveralModules

func testExtensionsFromSeveralModules() throws {
    let derived = Derived(name: "d")
    try assertEquals(actual: derived.fromFirst(), expected: "first d")
    try assertEquals(actual: derived.fromSecond(), expected: "second d")
    try assertEquals(actual: derived.fromFramework(), expected: "framework d")

    let receiver = Receiver(name: "r")
    try assertEquals(actual: receiver.own(), expected: "own r")
    try assertEquals(actual: receiver.fromFirst(), expected: "first r")
    try assertEquals(actual: receiver.fromSecond(), expected: "second r")
    try assertEquals(actual: receiver.fromFramework(), expected: "framework r")
}

// -------- Execution of the test --------

class ExtensionsFromSeveralModulesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("extensionsFromSeveralModules", testExtensionsFromSeveralModules)
    }
}
