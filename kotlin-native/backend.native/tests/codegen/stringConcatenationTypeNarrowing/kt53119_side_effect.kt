/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// The same tests are also tested with FileCheck in folder ../../filecheck/kt53119_*.kt
// Please keep them in sync

package codegen.stringConcatenationTypeNarrowing.kt53119_side_effect
import kotlin.test.*

fun withSideEffect(): String? {
    println("Side effect")
    return "Result."
}

@Test
fun runTest() {
    println("Retval is: " + withSideEffect())
    println("Retval is: " + withSideEffect() + "!")
    println(withSideEffect() + withSideEffect())
    println(withSideEffect() + withSideEffect() + withSideEffect())
}
