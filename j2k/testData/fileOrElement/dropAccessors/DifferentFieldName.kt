class AAA {
    var x = 42
        private set

    fun foo(other: AAA) {
        println(x)
        println(other.x)
        x = 10
    }
}
