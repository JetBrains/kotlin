// DUMP_IR
// WITH_STDLIB
// MUTE_LL_FIR
// ^ backend plugins are not executed -> declarations in dependent module are not visible

// MODULE: a
import org.jetbrains.kotlin.plugin.sandbox.AllPropertiesConstructor

class A(val s: String)
class B(val s: String)
class C(val s: String)

@AllPropertiesConstructor
open class Base {
    val a: A = A("a")
    val b = B("b")
}

// MODULE: b(a)
fun box(): String {
    // Verify generated properties are visible in module `a`'s metadata
    val base = Base()
    if (base.propertiesCount != 2) return "FAIL propertiesCount: ${base.propertiesCount}"
    with(base) {
        if ("x".extensionProp != "ext") return "FAIL extensionProp"
        if ("y".genericExtensionProp != "y") return "FAIL genericExtensionProp"
    }
    with("ctx") {
        if (base.contextProp != "ctx") return "FAIL contextProp"
    }

    return "OK"
}
