// IS_APPLICABLE: true

fun bar() {}

fun foo(f: Boolean) {
    <caret>if (f) bar() else bar()
}