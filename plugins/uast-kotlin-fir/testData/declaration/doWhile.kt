fun test() {
    var x : String? = null
    do {
        x = "non-null"
    } while (x != null)
}

fun kt44412() {
    var i = 0
    Outer@while (true) {
        ++i
        var j = 0
        Inner@do {
            ++j
        } while (if (j >= 3) false else break) // break@Inner
        if (i == 3) break
    }
}
