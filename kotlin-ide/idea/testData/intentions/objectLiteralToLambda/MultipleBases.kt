// IS_APPLICABLE: false
// WITH_RUNTIME

interface I

fun foo(runnable: Runnable) {}

fun bar() {
    foo(<caret>object : Runnable, I {
        override fun run() {
        }
    })
}