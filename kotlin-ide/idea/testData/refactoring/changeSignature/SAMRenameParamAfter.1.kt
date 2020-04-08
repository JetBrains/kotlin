fun test() {
    JTest.samTest(SAM { (s, n) -> s + " " })
    JTest.samTest(SAM { (x, y) -> x + " " })
}