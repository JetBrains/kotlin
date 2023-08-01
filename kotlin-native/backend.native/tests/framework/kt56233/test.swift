import Kt56233

func threadRoutine(pointer: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer? {
    autoreleasepool {
        let f = pointer.bindMemory(to: (() -> ()).self, capacity: 1).pointee
        f()
    }
    return nil
}

func launchThreads(
    _ f: @convention(c) () -> (),
    threadCount: Int = 4
) throws {
    var threads: [pthread_t] = []
    for _ in 0..<threadCount {
        let fPtr = UnsafeMutablePointer<() -> ()>.allocate(capacity: 1)
        fPtr.initialize(to: f)
        var thread: pthread_t? = nil
        let result = pthread_create(&thread, nil, threadRoutine, fPtr)
        try assertEquals(actual: result, expected: 0)
        threads.append(thread!)
    }
    for thread in threads {
        pthread_join(thread, nil)
    }
}

func kt56233() {
#if AGGRESSIVE_GC
    let count = 50_000
#else
    let count = 50_000_000
#endif
    // Stress testing for race conditions.
    for _ in 0..<count {
        _ = Kt56233.SimpleEnum.two.ordinal
    }
}

// -------- Execution of the test --------

class TestTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Kt56233", { try launchThreads(kt56233) })
    }
}
