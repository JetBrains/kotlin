// KIND: STANDALONE
// MODULE: main
// SWIFT_EXPORT_CONFIG: packageRoot=org.kotlin.foo
// FILE: foo.kt
// Trampolines generated for the root package keep the `@discardableResult` attribute of the original declarations.
package org.kotlin.foo

fun discardable(): Int = 42

@IgnorableReturnValue
fun explicitlyIgnorable(): Int = 42

fun unitFunction() {}

fun nothingFunction(): Nothing = TODO()

fun Int.discardableExtension(): Int = this

val Int.extensionProperty: Int
    get() = this

var variable: Int = 0

class Clazz

// FILE: foo_marked.kt
@file:MustUseReturnValues
package org.kotlin.foo

fun checkable(): Int = 42

fun Int.checkableExtension(): Int = this

@IgnorableReturnValue
fun ignorableInMarkedFile(): Int = 42

// FILE: bar.kt
// Declarations outside of the root package don't get trampolines.
package org.kotlin.bar

fun discardableOutsideOfRoot(): Int = 42

// FILE: bar_marked.kt
@file:MustUseReturnValues
package org.kotlin.bar

fun checkableOutsideOfRoot(): Int = 42
