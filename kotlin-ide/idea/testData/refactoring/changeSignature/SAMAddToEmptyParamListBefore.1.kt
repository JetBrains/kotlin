fun test() {
    JTest.samTest(SAM { " " })
    JTest.samTest(SAM { -> " " })
    JTest.samTest(SAM { -> " " })
}