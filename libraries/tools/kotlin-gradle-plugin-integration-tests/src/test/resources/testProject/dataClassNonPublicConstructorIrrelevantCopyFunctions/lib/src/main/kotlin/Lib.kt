data class Data private constructor(val value: String)

fun copy(value: String = ""): Data = null!!
class IrrelevantClass {
    fun copy(value: String = ""): Data = null!!
}
