package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.testing.SilentLog;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class KotlinCompilationFailureExceptionTest {

    @Test
    public void testNoLocation() throws Exception {
        MavenPluginLogMessageCollector collector = new MavenPluginLogMessageCollector(new SilentLog());
        collector.report(CompilerMessageSeverity.ERROR, "Something went wrong", null);

        try {
            collector.throwKotlinCompilerException();
            fail();
        } catch (KotlinCompilationFailureException e) {
            assertEquals("Something went wrong", e.getLongMessage().trim());
        }
    }

    @Test
    public void testLocationNoLineNoColumn() throws Exception {
        MavenPluginLogMessageCollector collector = new MavenPluginLogMessageCollector(new SilentLog());
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", CompilerMessageLocation.create("myfile.txt", -1, -1, "nothing"));

        try {
            collector.throwKotlinCompilerException();
            fail();
        } catch (KotlinCompilationFailureException e) {
            assertEquals("myfile.txt: Error in file", e.getLongMessage().trim());
        }
    }

    @Test
    public void testLocationNoColumn() throws Exception {
        MavenPluginLogMessageCollector collector = new MavenPluginLogMessageCollector(new SilentLog());
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", CompilerMessageLocation.create("myfile.txt", 777, -1, "nothing"));

        try {
            collector.throwKotlinCompilerException();
            fail();
        } catch (KotlinCompilationFailureException e) {
            assertEquals("myfile.txt:[777] Error in file", e.getLongMessage().trim());
        }
    }

    @Test
    public void testLocationWithLocation() throws Exception {
        MavenPluginLogMessageCollector collector = new MavenPluginLogMessageCollector(new SilentLog());
        collector.report(CompilerMessageSeverity.ERROR, "Error in file", CompilerMessageLocation.create("myfile.txt", 777, 9, "nothing"));

        try {
            collector.throwKotlinCompilerException();
            fail();
        } catch (KotlinCompilationFailureException e) {
            assertEquals("myfile.txt:[777,9] Error in file", e.getLongMessage().trim());
        }
    }
}
