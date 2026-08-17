fun f(a: String?) {
    var p = a
    while (p != null) {
        var q: String? = p
        while (q != null) {
            q = null
        }
        p = q
    }
}

fun box(): String {
    f(null)
    f("hello")
    return "OK"
}
