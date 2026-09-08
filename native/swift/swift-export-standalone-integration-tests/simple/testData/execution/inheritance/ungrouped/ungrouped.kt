// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: ungrouped.kt

open class FunctionMemberBase {
    open val producer: () -> String = { "kotlin-producer" }
    open fun transform(mapper: (String) -> String): String = mapper("kotlin")
}

fun callProducer(value: FunctionMemberBase): String = value.producer()
fun callTransform(value: FunctionMemberBase, prefix: String): String = value.transform { "$prefix:$it" }

open class SideEffectBase {

    open val tag: String? = "kotlin-tag"
}


// Safe call on a value that a Swift override may have made null.
fun tagLength(value: SideEffectBase): Int = value.tag?.length ?: -1

fun tagOrDefault(value: SideEffectBase): String = value.tag ?: "kotlin-default"

data class DataPayload(val text: String, val number: Int)

enum class InheritanceMode {
    kotlinMode,
    swiftMode,
}

value class InlinePayload(val value: Int)

open class TypeRichBase {
    open fun mapData(value: DataPayload): DataPayload =
        DataPayload("kotlin:${value.text}", value.number + 1)

    open fun mapEnum(value: InheritanceMode): InheritanceMode = InheritanceMode.kotlinMode

    open fun mapInline(value: InlinePayload): InlinePayload = InlinePayload(value.value + 1)
}

fun callMapData(value: TypeRichBase, payload: DataPayload): DataPayload = value.mapData(payload)
fun callMapEnum(value: TypeRichBase, mode: InheritanceMode): InheritanceMode = value.mapEnum(mode)
fun callMapInline(value: TypeRichBase, payload: InlinePayload): InlinePayload = value.mapInline(payload)

fun callToString(value: FunctionMemberBase): String {
    return value.toString()
}
