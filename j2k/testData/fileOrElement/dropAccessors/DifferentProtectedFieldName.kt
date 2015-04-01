public open class AAA {
    public var x: Int = 42
        protected set

    public fun foo(other: AAA) {
        println(x)
        println(other.x)
        x = 10
    }
}

class BBB : AAA() {
    fun bar() {
        println(x)
        x = 10
    }
}