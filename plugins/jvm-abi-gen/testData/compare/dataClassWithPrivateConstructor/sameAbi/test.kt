package test

@ExposedCopyVisibility
data class Class private constructor(
    val publicProperty: Any,
)
