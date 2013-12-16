// KT-2388
package foo

var done = false

object foo {
    var timeoutId: Long = 1

    val callbackWrapper = {
        timeoutId = -1 as Long
        done = true
    }
}

fun box(): Boolean {
    foo.callbackWrapper()
    return foo.timeoutId == -1 as Long
}
