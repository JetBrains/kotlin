import Latin1

func testLatin1() throws {
    let latin1String = Latin1.Latin1Kt.latin1String as NSString
#if ENABLE_LATIN1
    // The way it's currently implemented, Latin1 Kotlin Strings are actually ASCII Strings.
    let latin1StringEncoding = NSASCIIStringEncoding
#else
    let latin1StringEncoding = NSUTF16StringEncoding
#endif
    try assertEquals(actual: latin1String.fastestEncoding, expected: latin1StringEncoding)
    let utf16String = Latin1.Latin1Kt.utf16String as NSString
    try assertEquals(actual: utf16String.fastestEncoding, expected: NSUTF16StringEncoding)
}

// -------- Execution of the test --------

class Latin1Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("latin1", testLatin1)
    }
}
