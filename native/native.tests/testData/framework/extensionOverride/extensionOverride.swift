import ExtensionOverride

class Overriding : Receiver {
    override func fromFramework() -> String {
        return "swift " + name
    }
}

func testExtensionOverride() throws {
    let overriding = Overriding(name: "o")
    try fail("Overriding an extension was accepted: \(overriding.fromFramework())")
}

// -------- Execution of the test --------

class ExtensionOverrideTests : SimpleTestProvider {
    override init() {
        super.init()

        test("extensionOverride", testExtensionOverride)
    }
}
