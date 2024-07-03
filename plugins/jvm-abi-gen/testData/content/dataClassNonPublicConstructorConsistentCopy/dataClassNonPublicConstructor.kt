package test

@ConsistentCopyVisibility
data class PrivateConstructor private constructor(val x: Int)

@ConsistentCopyVisibility
data class InternalConstructor internal constructor(val x: Int)
