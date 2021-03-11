data class IntStringPair(val x: Int, val s: String)

fun f(x: List<IntStringPair>) {
    x.forEach { (fir<caret>st, second) ->
    }
}

// TYPE: first -> <html>Int</html>
