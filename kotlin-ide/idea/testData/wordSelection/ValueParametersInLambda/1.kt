fun foo(f: (Int) -> Int) {}

fun test() {
    foo { <caret><selection>it</selection> -> it + 1 }
}