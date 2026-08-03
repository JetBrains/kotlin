import CompanionBlocksAndExtensions

func testCompanionBlocksAndExtensions() throws {
    Bar() // only really checking that the framework was built successfully
}

// -------- Execution of the test --------

class CompanionBlocksAndExtensionsTests : SimpleTestProvider {
    override init() {
        super.init()

        test("companionBlocksAndExtensions", testCompanionBlocksAndExtensions)
    }
}
