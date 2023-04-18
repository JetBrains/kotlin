// MUTED_WHEN: K1
package test

data class DataClass(
    val intProp: Int,
    val stringProp: String
) {
    val nonConstructorProp: Int = 0
}

data object DataObject