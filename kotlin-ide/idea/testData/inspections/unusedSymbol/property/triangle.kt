// See KT-13288

interface Inter {
    fun something(): String
}

abstract class Abstract {
    fun something() = "hi"
}

class Test: Abstract(), Inter {
}

fun main(args: Array<String>) {
    val x: Inter = Test()
    x.something()
}