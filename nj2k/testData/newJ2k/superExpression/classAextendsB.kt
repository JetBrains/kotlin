internal open class B(i: Int) {
    internal open fun call(): Int {
        return 1
    }
}

internal class A : B(10) {
    public override fun call(): Int {
        return super.call()
    }
}