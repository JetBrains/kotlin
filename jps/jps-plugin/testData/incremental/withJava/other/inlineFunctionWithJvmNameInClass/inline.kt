package inline

class A {
    var z = 0

    @JvmName("fff")
    inline fun f(): Int = 0
}