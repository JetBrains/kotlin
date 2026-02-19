interface FooBase {
    @Deprecated(message = "message: hidden", level = DeprecationLevel.HIDDEN)
    fun hiddenLevel()

    fun descendantHiddenLevel()
}

class FooImpl() : FooBase {

    @Deprecated(message = "message: deprecatedVar", level = DeprecationLevel.HIDDEN)
    var deprecatedVar = 42

    @Deprecated(message = "message: deprecatedVal", level = DeprecationLevel.HIDDEN)
    val deprecatedVal = 42

    @Deprecated(message = "message: deprecatedConstructor", level = DeprecationLevel.HIDDEN)
    constructor(param: Int) : this()

    override fun hiddenLevel() {}

    @Deprecated(message = "message: descendantHidden", level = DeprecationLevel.HIDDEN)
    override fun descendantHiddenLevel() {
    }
}