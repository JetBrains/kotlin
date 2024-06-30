package test

// TODO: KT-69538
@ConsistentCopyVisibility
data class Class private constructor(
    val publicProperty: Any,
    private val privateProperty: Any,
)
