// this test will fail if control flow analysis will start to produce smart-casts after "while(2 * 2 == 4)"

fun x(): Boolean{}

fun foo(p: Any?) {
    while(2 * 2 == 4) {
        print(p!!)
        if (x()) break
    }

    <caret>p.hashCode()
}