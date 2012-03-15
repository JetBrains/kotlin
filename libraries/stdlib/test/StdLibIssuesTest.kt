package test.stdlib.issues

import java.util.List
import kotlin.util.*
import kotlin.test.*
import junit.framework.TestCase

private fun listDifference<T>(first : List<T>, second : List<T>) : List<T> {
    return first.filter{ !second.contains(it) }.toList()
}

class StdLibIssuesTest() : TestCase() {

    fun test_KT_1131() {
        val data = arrayList("blah", "foo", "bar")
        val filterValues = arrayList("bar", "something", "blah")

        expect(arrayList("foo")) {
            val answer = listDifference(data, filterValues)
            println("Found answer ${answer}")
            answer
        }
    }

}
