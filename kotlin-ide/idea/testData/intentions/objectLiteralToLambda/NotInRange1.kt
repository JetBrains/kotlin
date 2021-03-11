// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo(runnable: Runnable) {}

fun bar() {
    foo(object : Runnable <caret>{
        override fun run() {
        }
    })
}