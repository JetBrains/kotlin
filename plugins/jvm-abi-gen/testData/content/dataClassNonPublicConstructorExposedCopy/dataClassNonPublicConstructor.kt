package test

@ExposedCopyVisibility
data class PrivateConstructor private constructor(val x: Int)

@ExposedCopyVisibility
data class InternalConstructor internal constructor(val x: Int)
