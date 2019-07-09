internal open class B(i: Int) {
    internal open fun call(): Int {
        return 1
    }
}

internal class A : B(10) {

    override fun call(): Int {
        return super.call()
    }
}