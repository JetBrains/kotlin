package foo

class A() {

}

fun box(): Boolean {
    var a: A? = null
    when(a) {
        is A? -> return true
        else -> return false
    }
}