/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.AbstractClassBuilder
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.RecordComponentVisitor

class AsmVersionForJpsBuildTest : TestCase() {
    // Kotlin compiler contains some version of ASM that is taken from a particular platform dependency
    fun testAsmVersionForBundledKotlinCompiler() {
        val field = RecordComponentVisitor::class.java.getDeclaredField("api").also { it.isAccessible = true }
        val asmVersionForBundledCompiler = field.getInt(AbstractClassBuilder.EMPTY_RECORD_VISITOR)

        assertEquals(Opcodes.ASM9, asmVersionForBundledCompiler)
    }
}