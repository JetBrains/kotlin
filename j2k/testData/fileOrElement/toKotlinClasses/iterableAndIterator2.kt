// ERROR: Return type of 'iterator' is not a subtype of the return type of the overridden member 'public abstract fun iterator(): kotlin.Iterator<kotlin.String> defined in kotlin.Iterable'
// ERROR: Return type of 'iterator' is not a subtype of the return type of the overridden member 'public abstract fun iterator(): kotlin.Iterator<kotlin.String> defined in kotlin.Iterable'
package demo

import java.util.*

class Test : Iterable<String> {
    override fun iterator(): Iterator<String>? {
        return null
    }

    public fun push(i: Iterator<String>): Iterator<String> {
        val j = i
        return j
    }
}

class FullTest : Iterable<String> {
    override fun iterator(): Iterator<String>? {
        return null
    }

    public fun push(i: Iterator<String>): Iterator<String> {
        val j = i
        return j
    }
}