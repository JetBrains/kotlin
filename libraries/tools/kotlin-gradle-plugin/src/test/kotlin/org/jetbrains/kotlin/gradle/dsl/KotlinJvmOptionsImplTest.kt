package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.junit.Assert.*
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
        assertEquals("reportPerf", true, arguments.reportPerf)
        assertEquals("allowKotlinPackage", true, arguments.allowKotlinPackage)
        assertEquals("inheritMultifileParts", true, arguments.inheritMultifileParts)
        assertEquals("declarationsOutputPath", "declarationsPath", arguments.declarationsOutputPath)
        assertArrayEquals("scriptTemplates", arrayOf("a", "b", "c"), arguments.scriptTemplates)
    }
}