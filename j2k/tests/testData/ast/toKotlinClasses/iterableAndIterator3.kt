package demo

class Test() : Iterable<String> {
    override fun iterator(): Iterator<String> {
        return null
    }

    public fun push(i: Iterator<String>): Iterator<String> {
        val j = i
        return j
    }
}