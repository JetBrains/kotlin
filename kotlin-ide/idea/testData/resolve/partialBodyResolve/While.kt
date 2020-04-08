fun x(s: Any): Boolean{}

fun foo(p: Any?, p1: Any?) {
    while(x(p!!)) {
        print(p1!!)
    }

    <caret>p.hashCode()
}