package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinJvmOptionsTest {
    @Test
    fun testFreeArguments() {
        val options = KotlinJvmOptionsImpl()
        options.freeCompilerArgs = listOf(
                "-Xreport-perf",
                "-Xallow-kotlin-package",
                "-Xmultifile-parts-inherit",
                "-Xdump-declarations-to", "declarationsPath",
                "-script-templates", "a,b,c")

        val arguments = K2JVMCompilerArguments()
        options.updateArguments(arguments)
        assertEquals(options.freeCompilerArgs, arguments.freeArgs)
    }
}