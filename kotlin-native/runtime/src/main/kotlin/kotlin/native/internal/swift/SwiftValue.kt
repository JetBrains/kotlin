import kotlin.native.internal.ExportTypeInfo

@ExportTypeInfo("theSwiftValueTypeInfo")
open class SwiftValue {
    // TODO: These functions should virtually dispatch into corresponding swift functions via adapters.
    override fun toString(): String {
        return "SwiftValue_${hashCode()}"
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}