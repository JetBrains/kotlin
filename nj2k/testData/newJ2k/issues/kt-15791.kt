internal object A {
    @JvmStatic
    fun main(args: Array<String>) {
        MutableList::class.java.isAssignableFrom(ArrayList::class.java)
    }
}