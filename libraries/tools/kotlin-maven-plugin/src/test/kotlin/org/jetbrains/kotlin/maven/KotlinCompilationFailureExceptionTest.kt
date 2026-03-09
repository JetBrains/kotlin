package org.jetbrains.kotlin.maven

import org.apache.maven.plugin.testing.SilentLog
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.Companion.create
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import kotlin.test.*

class KotlinCompilationFailureExceptionTest {
    @Test
    fun testNoLocation() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(CompilerMessageSeverity.ERROR, "Something went wrong", null)

        try {
            collector.throwKotlinCompilerException()
            fail()
        } catch (e: KotlinCompilationFailureException) {
            assertEquals("Something went wrong", e.getLongMessage().trim { it <= ' ' })
        }
    }

    @Test
    fun testLocationNoLineNoColumn() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", create("myfile.txt", -1, -1, "nothing"))

        try {
            collector.throwKotlinCompilerException()
            fail()
        } catch (e: KotlinCompilationFailureException) {
            assertEquals("myfile.txt: Error in file", e.getLongMessage().trim { it <= ' ' })
        }
    }

    @Test
    fun testLocationNoColumn() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", create("myfile.txt", 777, -1, "nothing"))

        try {
            collector.throwKotlinCompilerException()
            fail()
        } catch (e: KotlinCompilationFailureException) {
            assertEquals("myfile.txt:[777] Error in file", e.getLongMessage().trim { it <= ' ' })
        }
    }

    @Test
    fun testLocationWithLocation() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", create("myfile.txt", 777, 9, "nothing"))

        try {
            collector.throwKotlinCompilerException()
            fail()
        } catch (e: KotlinCompilationFailureException) {
            assertEquals("myfile.txt:[777,9] Error in file", e.getLongMessage().trim { it <= ' ' })
        }
    }
}