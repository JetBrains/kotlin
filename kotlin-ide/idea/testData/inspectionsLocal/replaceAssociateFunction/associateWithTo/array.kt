// PROBLEM: none
// WITH_RUNTIME
fun getValue(i: Int): String = ""

fun associateWithTo() {
    val destination = mutableMapOf<Int, String>()
    arrayOf(1).<caret>associateTo(destination) { it to getValue(it) }
}
