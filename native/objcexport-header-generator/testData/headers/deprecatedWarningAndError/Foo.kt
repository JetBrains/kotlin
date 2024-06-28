class Foo constructor() {

    @Deprecated(message = "message: warning-property", level = DeprecationLevel.ERROR)
    var varError = 42

    @Deprecated(message = "message: warning-property", level = DeprecationLevel.WARNING)
    var varWarning = 42

    @Deprecated(message = "message: warning-constructor", level = DeprecationLevel.WARNING)
    constructor(paramA: Int, paramB: Int) : this()

    @Deprecated(message = "message: error-constructor", level = DeprecationLevel.ERROR)
    constructor(param: Int) : this()

    @Deprecated(message = "message: error", level = DeprecationLevel.ERROR)
    fun fooError() = Unit

    @Deprecated(message = "message: warning", level = DeprecationLevel.WARNING)
    fun fooWarning() = Unit
}

@Deprecated(message = "message: error-class", level = DeprecationLevel.ERROR)
class FooError

@Deprecated(message = "message: warning-class", level = DeprecationLevel.WARNING)
class FooWarning