package foo

class A() {
    fun lold() = true
    val p: ()->Boolean
    {
        $p = {{lold()}()}
    }
}


fun box(): Boolean {
    return A().p()
}

fun main(arg: Array<String>) {
    println(box())
}