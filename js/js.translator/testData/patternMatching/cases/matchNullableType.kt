package foo

class A() {

}

fun box(): Boolean {
    var a = null : A?
    when(a) {
        is A? -> return true
        else -> return false
    }
}