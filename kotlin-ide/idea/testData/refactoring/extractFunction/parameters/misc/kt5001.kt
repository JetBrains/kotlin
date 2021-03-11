fun Int.bar(n: Int): Boolean {
    return true
}

// SIBLING:
fun main(args: Array<String>) {
    val t = if (args.size > 0) {
        <selection>val al = 0
        al.bar(1)</selection>
    }
    else false
}