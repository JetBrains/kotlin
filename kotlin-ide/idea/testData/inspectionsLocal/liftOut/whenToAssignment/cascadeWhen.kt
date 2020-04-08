// WITH_RUNTIME
fun test(x: Any) {
    var res: String
    <caret>when (x) {
        is String ->
            if (x.length > 3) res = "long string"
            else res = "short string"
        is Int ->
            if (x > 999 || x < -99) res = "long int"
            else res = "short int"
        is Long ->
            TODO()
        else ->
            res = "I don't know"
    }
}