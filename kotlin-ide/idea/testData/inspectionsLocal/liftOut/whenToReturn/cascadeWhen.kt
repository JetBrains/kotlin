// WITH_RUNTIME
fun test(x: Any): String {
    <caret>when (x) {
        is String ->
            if (x.length > 3) return "long string"
            else return "short string"
        is Int ->
            if (x > 999 || x < -99) return "long int"
            else return "short int"
        is Long ->
            TODO()
        else ->
            return "I don't know"
    }
}