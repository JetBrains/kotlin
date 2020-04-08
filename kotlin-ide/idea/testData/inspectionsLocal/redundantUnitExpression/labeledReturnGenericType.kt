fun <T> foo(f: () -> T) {}

fun test() {
    foo {
        return@foo Unit<caret>
    }
}