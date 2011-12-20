namespace std.util

/**
Executes current block and returns elapsed time in milliseconds
*/
fun millisTime(block: fun() : Unit) : Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}