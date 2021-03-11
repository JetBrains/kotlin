// PROBLEM: none
inline fun foo(f: () -> Unit) {}

fun test(): Int {
    foo {
        fun bar() {
            return<caret>
        }
    }
    return 1
}