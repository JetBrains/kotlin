// ERROR: Return type of 'iterator' is not a subtype of the return type of the overridden member 'public abstract operator fun iterator(): kotlin.Iterator<kotlin.String> defined in kotlin.Iterable'
package demo

internal class Test : Iterable<String> {
    override fun iterator(): Iterator<String>? {
        return null
    }

    fun push(i: Iterator<String>): Iterator<String> {
        val j = i
        return j
    }
}
