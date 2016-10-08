object A {
    @JvmStatic fun main(args: Array<String>) {
        println(Unit::class.java)
        println(Boolean::class.java)
        println(Int::class.java)
        println(Double::class.java)
        println(IntArray::class.java)
        println(Array<Any>::class.java)
        println(Array<Array<Any>>::class.java)
    }
}
