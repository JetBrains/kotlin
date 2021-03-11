fun foo(<warning>u</warning> : Unit) : Int = 1

fun test() : Int {
    foo(<error>1</error>)
    val <warning>a</warning> : () -> Unit = {
        foo(<error>1</error>)
    }
    return 1
}