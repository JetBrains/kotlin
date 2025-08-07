package kotlin

@PublishedApi
internal fun throwUninitializedPropertyAccessException(name: String): Nothing {
    throw UninitializedPropertyAccessException("lateinit property $name has not been initialized")
}

@PublishedApi
internal fun throwUnsupportedOperationException(message: String): Nothing {
    throw UnsupportedOperationException(message)
}
