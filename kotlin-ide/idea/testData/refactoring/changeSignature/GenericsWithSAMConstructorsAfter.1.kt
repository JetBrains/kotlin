fun test() {
    SamTest.test(Foo<String, Int> { s, n -> "" })
    SamTest.test(Foo { s: MutableList<X<Int>>, n: X<MutableSet<String>> -> "" })
    SamTest.test(Foo { s: MutableList<X<Int>>, n: X<MutableSet<String>> -> "" })
}