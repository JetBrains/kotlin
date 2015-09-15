internal open class B internal constructor(i: Int) {
    internal open fun call(): Int {
        return 1
    }
}

internal class A internal constructor() : B(10) {

    override fun call(): Int {
        return super.call()
    }
}