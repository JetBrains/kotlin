package inline

@PublishedApi
internal class PublishedClass() {
    fun getValue() = 0
}

class A {
    @PublishedApi
    internal val publishedVal: Int
        get() = 1

    @PublishedApi
    internal fun publishedMethod(): Int {
        return 2
    }

    @PublishedApi
    internal var publishedVar: Int = 3

    companion object {
        @PublishedApi
        internal const val publishedConst: Int = 4
    }
}
