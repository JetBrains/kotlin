// ERROR: Null can not be a value of a non-null type Iterator<String?>
// ERROR: Null can not be a value of a non-null type Iterator<String?>
package demo

import java.util.*

internal class Test : Iterable<String?> {
    override fun iterator(): Iterator<String?> {
        return null
    }

    fun push(i: Iterator<String>): Iterator<String> {
        return i
    }
}

internal class FullTest : Iterable<String?> {
    override fun iterator(): Iterator<String?> {
        return null
    }

    fun push(i: Iterator<String>): Iterator<String> {
        return i
    }
}