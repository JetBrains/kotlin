class A {
    public fun foo(p: Int) {
        println("p = [$p]")
    }

    synchronized public fun foo() {
        foo(calcSomething())
    }

    // this method should be invoked under synchronized block!
    private fun calcSomething(): Int {
        return 0
    }
}
