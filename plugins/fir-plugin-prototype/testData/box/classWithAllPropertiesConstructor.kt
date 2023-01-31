// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// DUMP_IR
// WITH_STDLIB
// WITH_REFLECT
// FULL_JDK

// MODULE: a
import org.jetbrains.kotlin.fir.plugin.AllPropertiesConstructor

class A(val s: String)
class B(val s: String)
class C(val s: String)

@AllPropertiesConstructor
open class Base {
    val a: A = A("a")
    val b = B("b")
}

// MODULE: b(a)
// FILE: Derived.kt
import org.jetbrains.kotlin.fir.plugin.AllPropertiesConstructor

@AllPropertiesConstructor
class Derived : Base() {
    val c = C("c")
}

// FILE: main.kt
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaConstructor

fun box(): String {
    val constructor = Derived::class.constructors.first { it.valueParameters.size == 3 }.javaConstructor!!
    val derived = constructor.newInstance(A("a"), B("b"), C("c"))
    return with (derived) {
        if (a.s == "a" && b.s == "b" && c.s == "c") {
            "OK"
        } else {
            "Error"
        }
    }
}
