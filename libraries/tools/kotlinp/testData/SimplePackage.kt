// IGNORE K2

internal inline fun <reified X : Any> topLevelFun(x: X) = X::class

var topLevelProp: String? = null
    private set

typealias F<T, U> = Map<T, (StringBuilder) -> U?>

typealias G<S> = F<List<S>, Set<S>>
