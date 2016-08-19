package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.compiler.CompilationFailureException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class KotlinCompilationFailureException extends CompilationFailureException {
    private List<CompilerMessage> compilerMessages;

    public KotlinCompilationFailureException(@NotNull List<CompilerMessage> compilerMessages) {
        super(compilerMessages);
        this.compilerMessages = Collections.unmodifiableList(compilerMessages);
    }

    public List<CompilerMessage> getCompilerMessages() {
        return compilerMessages;
    }
}

