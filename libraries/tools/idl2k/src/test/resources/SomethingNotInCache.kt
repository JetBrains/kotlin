public external open class SomethingNotInCache {
    open val someReadOnlyParam: dynamic
    var someWriteableParam: dynamic
    fun someEmptyMethod(): String
    fun someMethod(root: dynamic): String
    fun optionalUsvStringFetcher(name: String): String?
}

