/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class CompilationException extends Exception {

  private final Collection<Message> myMessages;

  public static class Message {
    @NotNull
    private final CompilerMessageCategory myCategory;
    @NotNull
    private final String myMessage;
    @Nullable
    private final String myUrl;
    private final int myLine;
    private final int myColumn;

    public Message(CompilerMessageCategory category, String message) {
      this(category, message, null, -1, -1);
    }

    public Message(@NotNull CompilerMessageCategory category, @NotNull String message, @Nullable String url, int line, int column) {
      myCategory = category;
      myMessage = message;
      myUrl = url;
      myLine = line;
      myColumn = column;
    }

    @NotNull
    public CompilerMessageCategory getCategory() {
      return myCategory;
    }

    @NotNull
    public String getText() {
      return myMessage;
    }

    @Nullable
    public String getUrl() {
      return myUrl;
    }

    public int getLine() {
      return myLine;
    }

    public int getColumn() {
      return myColumn;
    }
  }

  public CompilationException(String message) {
    this(message, Collections.emptyList());
  }

  public CompilationException(String message, Collection<Message> messages) {
    super(message);
    myMessages = messages;
  }

  @NotNull
  public Collection<Message> getMessages() {
    return myMessages;
  }
}
