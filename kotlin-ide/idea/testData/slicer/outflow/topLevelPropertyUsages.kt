// FLOW: OUT

val <caret>x = 1

val y = x

fun test() {
    val y = x

    val z: Int

    init {
        z = x

        bar(x)
    }
}

fun bar(m: Int) {

}