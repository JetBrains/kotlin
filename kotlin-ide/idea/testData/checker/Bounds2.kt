fun test() {
    foo<<error>Int?</error>>()
    foo<Int>()
    bar<Int?>()
    bar<Int>()
    bar<<error>Double?</error>>()
    bar<<error>Double</error>>()
    1.buzz<<error>Double</error>>()
}

fun <T : Any> foo() {}
fun <T : Int?> bar() {}
fun <T : <warning>Int</warning>> Int.buzz() : Unit {}
