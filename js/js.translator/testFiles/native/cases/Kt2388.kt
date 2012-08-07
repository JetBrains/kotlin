package foo

native
fun _setTimeout(callback:()->Unit):Unit = noImpl

var done = false

object foo {
    var timeoutId: Long = 1

    val callbackWrapper = {
        timeoutId = -1 as Long
        done = true
    }
}

fun box(): Boolean {
    _setTimeout(foo.callbackWrapper)
    return foo.timeoutId == -1 as Long
}
