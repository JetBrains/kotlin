fun foo(f: (Int) -> Int) {}

fun test() {
    foo <selection>{ it -> <caret>it + 1 }</selection>
}