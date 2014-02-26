package foo

class A() {

}

class B() {

}

fun box(): Boolean {
    var c: Int = 0
    var a = A() : Any?
    var b = null : Any?
    when(a) {
        null -> c = 10;
        is B -> c = 10000
        is A -> c = 20;
        else -> c = 1000
    }
    when(b) {
        null -> c += 5
        is B -> c += 100
        else -> c = 1000
    }
    return (c == 25)
}