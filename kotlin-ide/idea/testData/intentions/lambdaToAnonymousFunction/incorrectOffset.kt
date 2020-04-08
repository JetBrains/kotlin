// IS_APPLICABLE: false

fun foo(f: () -> String) {}

fun test() {
    foo { -> "" <caret>}
}