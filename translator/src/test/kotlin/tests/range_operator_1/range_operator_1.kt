fun range_operator_1(): Int {
    val progression = 10..20
    val a = progression.iterator()
    var result = 0
    while (a.hasNext()) {
        val nxt = a.nextInt()
        result += nxt
    }
    return result
}