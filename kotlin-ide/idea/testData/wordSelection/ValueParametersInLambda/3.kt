fun foo(f: (Int) -> Int) {}

fun test() {
    foo <selection>{ <caret>it -> it + 1 }</selection>
}