// FIR_IDENTICAL
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

@Serializable
open class Parent(open val arg: Int)

<!DUPLICATE_SERIAL_NAME("arg")!>@Serializable<!>
class Derived(override val arg: Int): Parent(arg)

<!DUPLICATE_SERIAL_NAME("c")!>@Serializable<!>
class Regular(
    @SerialName("c") val a: Int,
    @SerialName("c") val b: Int
)

const val X = "X"
const val Y = "Y"

<!DUPLICATE_SERIAL_NAME("XY")!>@Serializable<!>
class WithConstEval(
    @SerialName(X + Y) val x: Int,
    @SerialName(X + Y) val y: Int
)
