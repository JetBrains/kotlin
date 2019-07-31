package test

class TestAssignmentInReturn {
    private var last: String? = null
    fun foo(s: String): String {
        return s.also { last = it }
    }
}