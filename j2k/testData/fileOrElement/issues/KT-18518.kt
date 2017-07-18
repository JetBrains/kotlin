object TestJava {
    fun main() {
        println(((Integer.MAX_VALUE + Integer.MAX_VALUE).toDouble() + 2.0).toInt())
        println(((Integer.valueOf(Integer.MAX_VALUE)!! + Integer.valueOf(Integer.MAX_VALUE)!!).toDouble() + 2.0).toInt())
    }
}