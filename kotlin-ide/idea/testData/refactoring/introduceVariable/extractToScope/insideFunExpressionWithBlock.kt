fun foo(f: () -> Int) { }

fun test() {
    foo(fun(): Int { return (<selection>1 + 2</selection>) * 3 })
}