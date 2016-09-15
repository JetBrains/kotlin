open class Base<T> {
    @JvmField
    val f: T = null!!

    fun m(t: T): T = null!!
}

class Impl<T> : Base<T>() {
    fun implM(t: T): T = null!!
}

annotation class Anno

@Anno
class Test {
    val f = Impl<String>()
}