/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;

public class MavenPluginLogMessageCollector implements MessageCollector {
    private final Log log;

    public MavenPluginLogMessageCollector(Log log) {
        this.log = log;
    }

    @Override
    public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
        String path = location.getPath();
        String position = path == null ? "" : path + ": (" + (location.getLine() + ", " + location.getColumn()) + ") ";

        String text = position + message;

        if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
            log.debug(text);
        } else if (CompilerMessageSeverity.ERRORS.contains(severity)) {
            log.error(text);
        } else if (severity == CompilerMessageSeverity.INFO) {
            log.info(text);
        } else {
            log.warn(text);
        }
    }
}
