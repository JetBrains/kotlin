open class AAA {
    var x: Int = 42
        protected set

    fun foo(other: AAA) {
        println(x)
        println(other.x)
        x = 10
    }
}

internal class BBB : AAA() {
    internal fun bar() {
        println(x)
        x = 10
    }
}