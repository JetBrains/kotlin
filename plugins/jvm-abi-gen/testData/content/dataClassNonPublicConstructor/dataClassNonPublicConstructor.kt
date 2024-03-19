package test

@ConsistentDataCopyVisibility
data class PrivateConstructor private constructor(val x: Int)

@ConsistentDataCopyVisibility
data class InternalConstructor internal constructor(val x: Int)
