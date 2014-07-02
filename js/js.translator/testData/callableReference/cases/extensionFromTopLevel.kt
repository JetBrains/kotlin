package foo

class A {
    var xx: Int = 100
}

fun A.bar(x: Int): Int {
    this.xx = this.xx * 2
    return x
}

fun box(): Boolean {
    val funRef = A::bar
    val obj = A()
    var result = obj.(funRef)(25)
    return result == 25 && obj.xx == 200
}