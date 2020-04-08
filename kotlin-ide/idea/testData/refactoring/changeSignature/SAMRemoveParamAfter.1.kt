fun test() {
    JTest.samTest(SAM { n -> s + " " })
    JTest.samTest(SAM { y -> x + " " })
}