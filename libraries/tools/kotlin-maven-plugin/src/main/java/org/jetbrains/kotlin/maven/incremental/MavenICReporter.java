/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.maven.incremental;

import kotlin.jvm.functions.Function0;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.incremental.ICReporter;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MavenICReporter implements ICReporter {
    private static final String IC_LOG_LEVEL_PROPERTY_NAME = "kotlin.compiler.incremental.log.level";
    private static final String IC_LOG_LEVEL_NONE = "none";
    private static final String IC_LOG_LEVEL_INFO = "info";
    private static final String IC_LOG_LEVEL_DEBUG = "debug";

    public static MavenICReporter get(@NotNull final Log log) {
        String logLevel = System.getProperty(IC_LOG_LEVEL_PROPERTY_NAME);
        if (logLevel == null) {
            if (log.isDebugEnabled()) {
                logLevel = IC_LOG_LEVEL_DEBUG;
            }
            else {
                logLevel = IC_LOG_LEVEL_NONE;
            }
        }

        if (logLevel.equalsIgnoreCase(IC_LOG_LEVEL_INFO)) {
            return MavenICReporter.info(log);
        }
        else if (logLevel.equalsIgnoreCase(IC_LOG_LEVEL_DEBUG)) {
            return MavenICReporter.debug(log);
        }
        else {
            if (!logLevel.equalsIgnoreCase(IC_LOG_LEVEL_NONE)) {
                log.warn("Unknown incremental compilation log level '" + logLevel + "'," +
                        "possible values: " + IC_LOG_LEVEL_NONE + ", " + IC_LOG_LEVEL_INFO + ", " + IC_LOG_LEVEL_DEBUG);
            }

            return MavenICReporter.noLog();
        }
    }

    private static MavenICReporter info(@NotNull final Log log) {
        return new MavenICReporter() {
            @Override
            protected boolean isLogEnabled() {
                return log.isInfoEnabled();
            }

            @Override
            protected void log(String str) {
                log.info(str);
            }
        };
    }

    private static MavenICReporter debug(@NotNull final Log log) {
        return new MavenICReporter() {
            @Override
            protected boolean isLogEnabled() {
                return log.isDebugEnabled();
            }

            @Override
            protected void log(String str) {
                log.debug(str);
            }
        };
    }

    private static MavenICReporter noLog() {
        return new MavenICReporter();
    }

    @NotNull
    private final Set<File> compiledKotlinFiles = new HashSet<>();

    protected boolean isLogEnabled() {
        return false;
    }

    protected void log(String str) {
    }

    private MavenICReporter() {
    }

    @Override
    public void report(Function0<String> getMessage) {
        if (isLogEnabled()) {
            log(getMessage.invoke());
        }
    }

    @Override
    public void reportCompileIteration(Collection<? extends File> sourceFiles, ExitCode exitCode) {
        compiledKotlinFiles.addAll(sourceFiles);
        if (isLogEnabled()) {
            log("Kotlin compile iteration: " + pathsAsString(sourceFiles));
            log("Exit code: " + exitCode.toString());
        }
    }

    @NotNull
    @Override
    public String pathsAsString(Iterable<? extends File> files) {
        return ICReporter.DefaultImpls.pathsAsString(this, files);
    }

    @NotNull
    @Override
    public String pathsAsString(File... files) {
        return ICReporter.DefaultImpls.pathsAsString(this, files);
    }

    @NotNull
    public Set<File> getCompiledKotlinFiles() {
        return compiledKotlinFiles;
    }
}
