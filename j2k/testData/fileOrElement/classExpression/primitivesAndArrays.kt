public object A {
    JvmStatic public fun main(args: Array<String>) {
        println(Void.TYPE)
        println(Integer.TYPE)
        println(java.lang.Double.TYPE)
        println(IntArray::class.java)
        println(Array<Any>::class.java)
        println(Array<Array<Any>>::class.java)
    }
}
