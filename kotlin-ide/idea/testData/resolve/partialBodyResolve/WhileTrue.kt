fun x(): Boolean{}

fun foo(p: Any?) {
    while(true) {
        print(p!!)
        if (x()) break
    }

    <caret>p.hashCode()
}