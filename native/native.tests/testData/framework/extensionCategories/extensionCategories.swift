import ExtensionCategories

func testExtensionCategories() throws {
    let receiver = Receiver(name: "r")
    try assertEquals(actual: receiver.own(), expected: "own r")
    try assertEquals(actual: receiver.fromAlphaBeta(), expected: "alphaBeta r")
    try assertEquals(actual: receiver.fromAnotherBuild(), expected: "anotherBuild r")
    try assertEquals(actual: receiver.fromIncluded(), expected: "included r")
    try assertEquals(actual: receiver.fromFramework(), expected: "framework r")
    try assertEquals(actual: Local().fromFrameworkOnLocal(), expected: "local")
}

// -------- Execution of the test --------

class ExtensionCategoriesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("extensionCategories", testExtensionCategories)
    }
}
