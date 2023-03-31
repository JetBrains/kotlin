import PermanentObjects

func testPermanentObjects() throws {
    PermanentObjects.KnlibraryKt.assertIsPermanent()
    let stableRefsBefore = PermanentObjects.KnlibraryKt.stableRefsCount()
    autoreleasepool {
        for i in 0..<1000 {
            PermanentObjects.Permanent().counter += 1
        }
    }
    let stableRefsAfter = PermanentObjects.KnlibraryKt.stableRefsCount()
    try assertEquals(actual: PermanentObjects.Permanent().counter, expected: 1000)
    try assertEquals(actual: stableRefsAfter, expected: stableRefsBefore)
}

// -------- Execution of the test --------

class TestTests : SimpleTestProvider {
    override init() {
        super.init()

        test("permanentObjects", testPermanentObjects)
    }
}
