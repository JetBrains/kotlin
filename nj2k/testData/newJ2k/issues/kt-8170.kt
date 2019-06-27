object ArrayNullable {
    @JvmStatic
    fun main(args: Array<String>) {
        val notNull = 0
        val a1 = arrayOfNulls<Int>(2)
        a1[0] = null
        a1[1] = notNull
        println(a1[0])
        println(a1[1])
        val a2 = arrayOfNulls<Int>(2)
        a2[0] = nullable()
        a2[1] = notNull
        println(a2[0])
        println(a2[1])
        val a3 = arrayOf(null, nullable(), notNull)
        println(a3[0])
        println(a3[1])
        println(a3[2])
    }

    fun nullable(): Int? {
        return if (System.getProperty("user.home").length > 20) null else 1
    }
}
