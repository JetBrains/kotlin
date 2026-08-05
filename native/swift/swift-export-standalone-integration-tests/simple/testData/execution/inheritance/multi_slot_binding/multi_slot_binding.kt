// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: multi_slot_binding.kt

// Two shapes where a single member occupies two interface slots: two interfaces contributing the same
// defaulted signature, and one property declared by two interfaces that the Kotlin class implements once.

interface LeftDefault {
    fun conflict(): String = "left"
}

interface RightDefault {
    fun conflict(): String = "right"
}

open class ConflictAnchor

fun callLeftDefault(value: LeftDefault): String = value.conflict()
fun callRightDefault(value: RightDefault): String = value.conflict()

data class DataPayload(val text: String, val number: Int)

interface RichDataView {
    val richData: DataPayload
}

interface RichDataMirrorView {
    val richData: DataPayload
}

open class TypeRichSuperBase : RichDataView, RichDataMirrorView {
    override open val richData: DataPayload = DataPayload("kotlin-super", 40)
}

fun readRichData(value: RichDataView): DataPayload = value.richData
fun readMirroredRichData(value: RichDataMirrorView): DataPayload = value.richData
fun richDataAsView(value: TypeRichSuperBase): RichDataView = value
fun richDataAsMirrorView(value: TypeRichSuperBase): RichDataMirrorView = value
