// ERROR: Smart cast to 'String' is impossible, because 'last' is a mutable property that could have been changed by this time
package test

class TestAssignmentInReturn {
    private var last: String? = null

    fun foo(s: String): String {
        last = s
        return last
    }
}