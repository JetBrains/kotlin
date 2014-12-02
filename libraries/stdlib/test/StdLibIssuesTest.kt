package test.stdlib.issues

import kotlin.test.*
import org.junit.Test as test

private fun listDifference<T>(first : List<T>, second : List<T>) : List<T> {
    return first.filter{ !second.contains(it) }.toList()
}

class StdLibIssuesTest {

    test fun test_KT_1131() {
        val data = arrayListOf("blah", "foo", "bar")
        val filterValues = arrayListOf("bar", "something", "blah")

        expect(arrayListOf("foo")) {
            val answer = listDifference(data, filterValues)
            println("Found answer ${answer}")
            answer
        }
    }

}
