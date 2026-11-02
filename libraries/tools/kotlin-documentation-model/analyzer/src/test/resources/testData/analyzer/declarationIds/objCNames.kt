// ObjC selector names participate in declaration IDs for callables imported from ObjC (own or inherited
// `kotlinx.cinterop.ObjCMethod`/`ObjCConstructor`/`ObjCFactory`): the selector consists of the function
// name and parameter names except the first, mirroring
// FirNativeObjcOverrideApplicabilityChecker.hasDifferentParameterNames. `@kotlin.native.ObjCName` plays
// no role. The annotations are stubbed in-module, as the JVM classpath has no actualization of the
// common expect declarations. In-module sources cannot declare members of the `kotlin` package unless
// ALLOW_KOTLIN_PACKAGE lifts the source-session restriction.

// MODULE: main
// ALLOW_KOTLIN_PACKAGE
// FILE: kotlinx/cinterop/ObjC.kt
package kotlinx.cinterop

public annotation class ObjCMethod
public annotation class ObjCConstructor
public annotation class ObjCFactory

// FILE: kotlin/native/ObjCName.kt
package kotlin.native

public annotation class ObjCName(val name: String = "", val swiftName: String = "", val exact: Boolean = false)

// FILE: sample/ObjCDeclarations.kt
package sample

import kotlin.native.ObjCName
import kotlinx.cinterop.ObjCConstructor
import kotlinx.cinterop.ObjCFactory
import kotlinx.cinterop.ObjCMethod

@ObjCMethod
fun plain(first: String, second: Int) {}

// cinterop factory functions are selector-sensitive too.
@ObjCFactory
fun create(first: Int, second: Int) {}

open class Base {
    @ObjCMethod
    open fun foo(first: String, second: String) {}
}

// The override's own parameter names are compared directly; there is no inheritance from the base.
class Derived : Base() {
    override fun foo(first: String, second: String) {}
}

class Constructed @ObjCConstructor constructor(first: Int, second: Int)

// `@ObjCName` does not make a callable selector-sensitive, even on its parameters.
@ObjCName("power", "swiftPower")
fun power(@ObjCName("input") base: Int, exponent: Int) {}

// No ObjC annotations: not selector-sensitive.
fun control(value: Int) {}
