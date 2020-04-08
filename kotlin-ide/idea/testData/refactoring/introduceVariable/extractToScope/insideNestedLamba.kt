fun foo(f: () -> Int) = f()

fun test() {
    foo { foo { (<selection>1 + 2</selection>) * 3 } }
}