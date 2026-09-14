// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f11_addrspace_lowering

import kotlin.native.runtime.GC

class Person(var name: String, var age: Int)

fun box(): String {
    val p = Person("Alice", 30)
    p.name = "Alice Smith"
    p.age = 31

    GC.collect()

    if (p.name != "Alice Smith" || p.age != 31) {
        return "FAIL heap field access: name=${p.name}, age=${p.age}"
    }
    return "OK"
}
