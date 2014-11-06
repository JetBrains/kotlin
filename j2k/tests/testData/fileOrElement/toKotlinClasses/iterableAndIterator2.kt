// ERROR: Null can not be a value of a non-null type kotlin.Iterator<kotlin.String>
// ERROR: Null can not be a value of a non-null type kotlin.Iterator<kotlin.String>
package demo

import java.util.*

class Test : Iterable<String> {
    override fun iterator(): Iterator<String> {
        return null
    }

    public fun push(i: Iterator<String>): Iterator<String> {
        val j = i
        return j
    }
}

class FullTest : Iterable<String> {
    override fun iterator(): Iterator<String> {
        return null
    }

    public fun push(i: Iterator<String>): Iterator<String> {
        val j = i
        return j
    }
}