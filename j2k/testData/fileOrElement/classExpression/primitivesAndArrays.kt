import kotlin.platform.platformStatic

public object A {
    platformStatic public fun main(args: Array<String>) {
        println(Void.TYPE)
        println(Integer.TYPE)
        println(java.lang.Double.TYPE)
        println(javaClass<IntArray>())
        println(javaClass<Array<Any>>())
        println(javaClass<Array<Array<Any>>>())
    }
}
