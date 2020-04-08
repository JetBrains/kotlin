fun foo(f: () -> Int) { }

fun test() {
    foo(fun() = (<selection>1 + 2</selection>) * 3)
}