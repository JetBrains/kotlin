package streams.sequence.filter

fun main(args: Array<String>) {
    //Breakpoint!
    listOf("abs", "bcs", "abt").asSequence().dropWhile { it.startsWith('a') }.forEach {}
}