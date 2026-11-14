// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE

open class Base {
    open fun touch(): Int {
        return 0
    }
}

// FUNCTION_HAS_EFFECTS: function=parentParam WRITE
fun parentParam(base: Base) = base.touch()

class Leaf : Base()

// FUNCTION_HAS_EFFECTS: function=closedParam PURE
fun closedParam(value: Leaf) = value.touch()

fun box(): String {
    val leaf = Leaf()
    // we check so that these don't get removed by possible dce in the future
    if (parentParam(leaf) != 0) return "something's very wrong"
    if (closedParam(leaf) != 0) return "something's very wrong"
    return "OK"
}
