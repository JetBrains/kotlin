fun test() {
    JTest.samTest(SAM { n, s -> s + " " })
    JTest.samTest(SAM { n, s -> s + " " })
    JTest.samTest(SAM { n, it -> it + " " })
}