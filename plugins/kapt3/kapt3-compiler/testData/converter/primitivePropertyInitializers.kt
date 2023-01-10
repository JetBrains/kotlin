class RegularClass(
    val constructorProperty: Int = 1
) {
    val dependentProperty = constructorProperty
    val staticProperty = true

    companion object {
        val companionProperty = 1.0
        const val constCompanionProperty = 1.0f
    }
}

object Object {
    val objectProperty = 1.0
    @JvmField
    val objectFieldProperty = 'c'
    const val constObjectProperty = 1.0f
}