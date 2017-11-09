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

package org.jetbrains.kotlin.maven;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.log4j.Level;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public class IdeaCoreLoggerFactory implements Logger.Factory {

    @NotNull
    private final Log parent;

    public IdeaCoreLoggerFactory(@NotNull  Log parent) {
        this.parent = parent;
    }

    public IdeaCoreLoggerFactory() {
        this(new SystemStreamLog());
    }

    @NotNull
    @Override
    public Logger getLoggerInstance(@NotNull String s) {
        return new Logger() {
            @Override
            public boolean isDebugEnabled() {
                return parent.isDebugEnabled();
            }

            @Override
            public void debug(String s) {
                parent.debug(s);
            }

            @Override
            public void debug(@Nullable Throwable throwable) {
                parent.debug(s);
            }

            @Override
            public void debug(String s, @Nullable Throwable throwable) {
                parent.debug(s, throwable);
            }

            @Override
            public void info(String s) {
                parent.info(s);
            }

            @Override
            public void info(String s, @Nullable Throwable throwable) {
                parent.info(s, throwable);
            }

            @Override
            public void warn(String s, @Nullable Throwable throwable) {
                parent.warn(s, throwable);
            }

            @Override
            public void error(String s, @Nullable Throwable throwable, @NotNull String... strings) {
                String message = s;

                if (strings.length > 0) {
                    StringJoiner j = new StringJoiner("\n");

                    j.add(s);
                    j.add("details:");
                    for (String details : strings) {
                        j.add(details);
                    }

                    message = j.toString();
                }

                parent.error(message, throwable);
            }

            @Override
            public void setLevel(Level level) {
            }
        };
    }
}
