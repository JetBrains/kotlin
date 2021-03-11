// FLOW: OUT

fun test(<caret>o: Any) {
    val x = o as String
    val y = o as? String
}