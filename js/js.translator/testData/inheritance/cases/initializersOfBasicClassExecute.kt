package foo

open class A() {
    var a = 3;
}

class B() : A() {

}

fun box(): Int {
    return (B().a)
}
