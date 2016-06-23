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

import com.intellij.openapi.util.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MavenPluginLogMessageCollector implements MessageCollector {
    private final Log log;
    private final ArrayList<Pair<CompilerMessageLocation, String>> collectedErrors = new ArrayList<Pair<CompilerMessageLocation, String>>();

    public MavenPluginLogMessageCollector(Log log) {
        this.log = checkNotNull(log, "log shouldn't be null");
    }

    @Override
    public boolean hasErrors() {
        return !collectedErrors.isEmpty();
    }

    @NotNull
    public List<Pair<CompilerMessageLocation, String>> getCollectedErrors() {
        return Collections.unmodifiableList(collectedErrors);
    }

    @Override
    public void clear() {
        // Do nothing
    }

    @Override
    public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
        String path = location.getPath();
        String position = path == null ? "" : path + ": (" + (location.getLine() + ", " + location.getColumn()) + ") ";

        String text = position + message;

        if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
            log.debug(text);
        } else if (CompilerMessageSeverity.ERRORS.contains(severity)) {
            collectedErrors.add(new Pair<CompilerMessageLocation, String>(location, message));
            log.error(text);
        } else if (severity == CompilerMessageSeverity.INFO) {
            log.info(text);
        } else {
            log.warn(text);
        }
    }

    public void throwKotlinCompilerException() throws KotlinCompilationFailureException {
        throw new KotlinCompilationFailureException(
                CollectionsKt.map(getCollectedErrors(), new Function1<Pair<CompilerMessageLocation, String>, CompilerMessage>() {
                    @Override
                    public CompilerMessage invoke(Pair<CompilerMessageLocation, String> pair) {
                        CompilerMessageLocation location = pair.getFirst();
                        String message = pair.getSecond();
                        String lineContent = location.getLineContent();
                        int lineContentLength = lineContent == null ? 0 : lineContent.length();

                        return new CompilerMessage(
                                location.getPath(),
                                CompilerMessage.Kind.ERROR,
                                fixLocation(location.getLine()),
                                fixLocation(location.getColumn()),
                                fixLocation(location.getLine()),
                                Math.min(fixLocation(location.getColumn()), lineContentLength),
                                message
                        );
                    }
                })
        );
    }

    private static int fixLocation(int n) {
        if (n < 0) {
            return 0;
        }
        return n;
    }
}
