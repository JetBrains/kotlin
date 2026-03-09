package org.jetbrains.kotlin.maven

import org.apache.maven.plugin.testing.SilentLog
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity


class KotlinCompilationFailureExceptionTest {
    @org.junit.Test
    fun testNoLocation() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(CompilerMessageSeverity.ERROR, "Something went wrong", null)

        try {
            collector.throwKotlinCompilerException()
            org.junit.Assert.fail()
        } catch (e: KotlinCompilationFailureException) {
            org.junit.Assert.assertEquals("Something went wrong", e.getLongMessage().trim { it <= ' ' })
        }
    }

    @org.junit.Test
    fun testLocationNoLineNoColumn() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", CompilerMessageLocation.create("myfile.txt", -1, -1, "nothing"))

        try {
            collector.throwKotlinCompilerException()
            org.junit.Assert.fail()
        } catch (e: KotlinCompilationFailureException) {
            org.junit.Assert.assertEquals("myfile.txt: Error in file", e.getLongMessage().trim { it <= ' ' })
        }
    }

    @org.junit.Test
    fun testLocationNoColumn() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(
            CompilerMessageSeverity.ERROR,
            "Error in file",
            CompilerMessageLocation.create("myfile.txt", 777, -1, "nothing")
        )

        try {
            collector.throwKotlinCompilerException()
            org.junit.Assert.fail()
        } catch (e: KotlinCompilationFailureException) {
            org.junit.Assert.assertEquals("myfile.txt:[777] Error in file", e.getLongMessage().trim { it <= ' ' })
        }
    }

    @org.junit.Test
    fun testLocationWithLocation() {
        val collector = MavenPluginLogMessageCollector(SilentLog())
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", CompilerMessageLocation.create("myfile.txt", 777, 9, "nothing"))

        try {
            collector.throwKotlinCompilerException()
            org.junit.Assert.fail()
        } catch (e: KotlinCompilationFailureException) {
            org.junit.Assert.assertEquals("myfile.txt:[777,9] Error in file", e.getLongMessage().trim { it <= ' ' })
        }
    }
}
