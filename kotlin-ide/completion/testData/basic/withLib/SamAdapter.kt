import lib.JavaClass

class KotlinClass : JavaClass()

fun test() = KotlinClass().<caret>

// EXIST: { lookupString: "execute", itemText: "execute", tailText: "(Runnable!, Int)", typeText: "Unit", attributes: "" }
// EXIST: { lookupString: "execute", itemText: "execute", tailText: "((() -> Unit)!, Int)", typeText: "Unit", attributes: "" }
