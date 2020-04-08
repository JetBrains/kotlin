data class IntStringPair(val x: Int, val s: String)

fun f(x: IntStringPair) {
    val (fir<caret>st, second) = x
}

// TYPE: first -> <html>Int</html>
