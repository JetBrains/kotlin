package test

@ConsistentCopyVisibility
data class Class private constructor(
    val publicProperty: Any,
    private val privateProperty: Any,
)
