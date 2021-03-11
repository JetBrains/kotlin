fun foo(i: (Int) -> Unit) {}
fun test() {
    foo { <caret>x ->
        foo { x -> x % 2 == 0 }
    }
}
