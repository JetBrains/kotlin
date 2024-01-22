import PermanentObjects

func testPermanentObjects() throws {
    PermanentObjects.PermanentObjectsKt.assertIsPermanent()
    let stableRefsBefore = PermanentObjects.PermanentObjectsKt.stableRefsCount()
    autoreleasepool {
        for i in 0..<1000 {
            PermanentObjects.Permanent().counter += 1
        }
    }
    let stableRefsAfter = PermanentObjects.PermanentObjectsKt.stableRefsCount()
    try assertEquals(actual: PermanentObjects.Permanent().counter, expected: 1000)
    try assertEquals(actual: stableRefsAfter, expected: stableRefsBefore)
}

// -------- Execution of the test --------

class PermanentObjectsTests : SimpleTestProvider {
    override init() {
        super.init()

        test("permanentObjects", testPermanentObjects)
    }
}
