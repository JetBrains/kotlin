internal inline fun <reified X : Any> topLevelFun(x: X) = X::class

var topLevelProp: String? = null
    private set
