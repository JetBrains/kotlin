import BlockParamNames

func testBlockParamNames() throws {
    Tests.shared.testId { (_: Any, _: Any) in }
    Tests.shared.testCls { (_: Any, _: Any) in }
    Tests.shared.testOK { (_: Any, _: Any) in }
}

// -------- Execution of the test --------

class BlockParamNamesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("blockParamNames", testBlockParamNames)
    }
}
