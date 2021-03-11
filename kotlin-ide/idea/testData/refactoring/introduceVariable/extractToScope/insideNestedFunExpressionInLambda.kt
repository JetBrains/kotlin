fun foo(f: () -> Int) = f()

fun test() {
    foo { foo(fun() = (<selection>1 + 2</selection>) * 3) }
}