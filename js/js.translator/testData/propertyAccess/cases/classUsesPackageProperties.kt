package foo

var a = 0

class A() {
    {
        a++
    }

}

fun box(): Boolean {
    var c = A()
    c = A()
    c = A()
    return (a == 3)
}