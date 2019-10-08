// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION

@file:JsExport

package foo

external interface I<T, out S, in U> {
    var x: T
    val y: S
    fun z(u: U)
}

external interface I2 {
    var x: String
    val y: Boolean
    fun z(z: Int)
}

abstract class AC : I2 {
    override var x = "AC"
    override abstract val y: Boolean
    override abstract fun z(z: Int)

    val acProp: String = "acProp"
    abstract val acAbstractProp: String
}

open class OC(
    override val y: Boolean,
    override val acAbstractProp: String
) : AC(), I<String, Boolean, Int> {
    override fun z(z: Int) {
    }

    private val privateX: String = "privateX"
    private fun privateFun(): String = "privateFun"
}

final class FC : OC(true, "FC")