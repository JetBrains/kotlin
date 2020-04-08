// "Replace 'if' expression with elvis expression" "true"
// WITH_RUNTIME
class Test {
    var x: Any? = null

    fun test() {
        val i = if (x is String) foo(<caret>x) else bar()
    }

    fun foo(s: String) = 1

    fun bar() = 0
}