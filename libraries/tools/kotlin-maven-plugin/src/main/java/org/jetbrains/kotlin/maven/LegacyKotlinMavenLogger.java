/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.buildtools.api.KotlinLogger;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;

public class LegacyKotlinMavenLogger implements KotlinLogger {
    private final MessageCollector messageCollectorFallback;
    private final Log mavenLog;

    public LegacyKotlinMavenLogger(MessageCollector fallback, Log log) {
        messageCollectorFallback = fallback;
        mavenLog = log;
    }

    @Override
    public boolean isDebugEnabled() {
        return mavenLog.isDebugEnabled();
    }

    @Override
    public void error(@NotNull String s, @Nullable Throwable throwable) {
        messageCollectorFallback.report(CompilerMessageSeverity.ERROR, s, null);
    }

    @Override
    public void warn(@NotNull String s, @Nullable Throwable throwable) {
        messageCollectorFallback.report(CompilerMessageSeverity.WARNING, s, null);
    }

    @Override
    public void info(@NotNull String s) {
        messageCollectorFallback.report(CompilerMessageSeverity.INFO, s, null);
    }

    @Override
    public void debug(@NotNull String s) {
        messageCollectorFallback.report(CompilerMessageSeverity.LOGGING, s, null);
    }

    @Override
    public void lifecycle(@NotNull String s) {
        messageCollectorFallback.report(CompilerMessageSeverity.INFO, s, null);
    }
}
