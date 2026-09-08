// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance(CrossModuleSupport)
// FILE: cross_module_packages.kt

// Swift subclass -> Kotlin class in module Inheritance (package middle.pkg)
//                -> Kotlin class in module CrossModuleSupport (package base.pkg)

package middle.pkg

import base.pkg.CrossModuleBase

open class CrossModuleMiddle : CrossModuleBase() {
    open fun middleValue(): String = "kotlin-middle"
}

fun callMiddleValue(value: CrossModuleMiddle): String = value.middleValue()

fun callBaseValueFromMiddle(value: CrossModuleBase): String = value.baseValue()

// MODULE: CrossModuleSupport
// EXPORT_TO_SWIFT
// FILE: cross_module_support.kt

package base.pkg

open class CrossModuleBase {
    open fun baseValue(): String = "kotlin-base"
}

fun callBaseValue(value: CrossModuleBase): String = value.baseValue()
