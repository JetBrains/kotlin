fun test() {
    SamTest.test(Foo<String, Int> { s, n -> "" })
    SamTest.test(Foo { s: String, n: Int -> "" })
    SamTest.test(Foo { s: String, n: Int -> "" })
}