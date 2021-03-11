package npe

fun main(args: Array<String>) {
    val a = null
    try {
        //Breakpoint!
        a!!
    }
    catch (e: Exception) {
        val b = 1
    }
}
