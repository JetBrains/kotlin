// TARGET_BACKEND: JVM_IR
// DUMP_IR
// WITH_STDLIB
// WITH_REFLECT
// FULL_JDK

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

@AllPropertiesConstructor
class Container<T> {
    val tag: String = "default"
}

// MODULE: b(a)
// FILE: Derived.kt
import org.jetbrains.kotlin.plugin.sandbox.AllPropertiesConstructor

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
    with(derived) {
        with("") {
            hasExtension()
            hasContext()
        }
    }

    // The generated `outerTypeProp` property doesn't have proper initializer, so it's easier just to check its existance using reflection
    val outerRef: kotlin.reflect.KProperty1<Container<String>, String> = Container<String>::outerTypeProp
    if (outerRef.name != "outerTypeProp") return "FAIL outerTypeProp"

    return "OK"
}
