open class AAA {
    var x = 42
        protected set

    fun foo(other: AAA) {
        println(x)
        println(other.x)
        x = 10
    }
}

internal class BBB : AAA() {
    fun bar() {
        println(x)
        x = 10
    }
}