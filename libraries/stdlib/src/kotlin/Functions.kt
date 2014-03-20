package kotlin

public fun <T: Any> Function1<T, T?>.toGenerator(initialValue: T): Function0<T?> {
    var nextValue: T? = initialValue
    return {
        nextValue?.let { result ->
            nextValue = this@toGenerator(result)
            result
        }
    }
}

