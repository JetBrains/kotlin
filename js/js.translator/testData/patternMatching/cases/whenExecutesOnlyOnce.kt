package foo

class A() {

}

fun box(): Boolean {
    var a = 0
    when(A()) {
        is A -> a++;
        is A -> a++;
        else -> a++;
    }
    return (a == 1)
}