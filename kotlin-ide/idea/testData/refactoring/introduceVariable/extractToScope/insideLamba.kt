fun foo(f: () -> Int) { }

fun test() {
    foo { (<selection>1 + 2</selection>) * 3 }
}