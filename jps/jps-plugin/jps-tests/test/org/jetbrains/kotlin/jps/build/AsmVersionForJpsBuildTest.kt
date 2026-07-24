package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.codegen.AbstractClassBuilder
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.RecordComponentVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsmVersionForJpsBuildTest {
    // Kotlin compiler contains some version of ASM that is taken from a particular platform dependency
    @Test
    fun testAsmVersionForBundledKotlinCompiler() {
        val field = RecordComponentVisitor::class.java.getDeclaredField("api").also { it.isAccessible = true }
        val asmVersionForBundledCompiler = field.getInt(AbstractClassBuilder.EMPTY_RECORD_VISITOR)

        assertEquals(Opcodes.ASM9, asmVersionForBundledCompiler)
    }
}
