// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: nested_classes.kt

// Nested declaration relocation: Swift subclasses a nested Kotlin open class.

data class DataPayload(val text: String, val number: Int)

enum class InheritanceMode {
    kotlinMode,
    swiftMode,
}


interface NestedRichView {
    val nestedData: DataPayload
    val nestedMode: InheritanceMode
}

class NestedInheritanceContainer {
    open class NestedBase : NestedRichView {
        override open val nestedData: DataPayload = DataPayload("kotlin-nested-data", 50)
        override open val nestedMode: InheritanceMode = InheritanceMode.kotlinMode

        open fun nestedValue(): String = "kotlin-nested"
    }
}


fun callNestedValue(value: NestedInheritanceContainer.NestedBase): String = value.nestedValue()
fun nestedAsRichView(value: NestedInheritanceContainer.NestedBase): NestedRichView = value
fun readNestedData(value: NestedRichView): DataPayload = value.nestedData
fun readNestedMode(value: NestedRichView): InheritanceMode = value.nestedMode
