fun x(s: String): Collection<String>{}

fun foo(p: Any?, p1: Any?) {
    for (e in x(p as String)) {
        print(p1!!)
    }

    <caret>p.length()
}