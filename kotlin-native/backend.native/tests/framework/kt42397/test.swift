import Kt42397

class Results {
    var aFoo: Int32 = 0
    var bFoo: Int32 = 0
}

func runTestKt42397(pointer: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer? {
    autoreleasepool {
        KnlibraryKt.enableMemoryChecker()
        let results = pointer.bindMemory(to: Results.self, capacity: 1).pointee
        results.aFoo = A().foo()
        results.bFoo = B.Companion().foo()
    }

    return nil
}

func testKt42397() throws {
    let results = Results()
    let resultsPtr = UnsafeMutablePointer<Results>.allocate(capacity: 1)
    resultsPtr.initialize(to: results)
    var thread: pthread_t? = nil
    let result = pthread_create(&thread, nil, runTestKt42397, resultsPtr)
    try assertEquals(actual: result, expected: 0)
    pthread_join(thread!, nil)

    try assertEquals(actual: results.aFoo, expected: 1)
    try assertEquals(actual: results.bFoo, expected: 2)
}

// -------- Execution of the test --------

class TestTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Kt42397", testKt42397)
    }
}
