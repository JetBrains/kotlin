data class IntStringPair(val x: Int, val s: String)

fun f(x: List<IntStringPair>) {
    for ((fir<caret>st, second) in x) {
    }
}

// TYPE: first -> <html>Int</html>
