// ERROR: Null can not be a value of a non-null type kotlin.Iterator<kotlin.String>
package demo

class Test : Iterable<String> {
    override fun iterator(): Iterator<String> {
        return null
    }

    public fun push(i: Iterator<String>): Iterator<String> {
        val j = i
        return j
    }
}