// PLATFORM_DEPENDANT_METADATA
// DUMP_KT_IR
package test

import org.jetbrains.kotlin.fir.plugin.AddAnnotations

@AddAnnotations
class Some(val x: Int) {
    fun foo() {}

    // some comment
    class Derived
}
