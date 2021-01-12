// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import org.gradle.internal.impldep.com.google.gson.GsonBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.Message.FilePosition;
import org.jetbrains.plugins.gradle.tooling.Message.Kind;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.util.ExceptionUtilRt.findCause;
import static org.jetbrains.plugins.gradle.tooling.Message.Kind.INFO;
import static org.jetbrains.plugins.gradle.tooling.Message.Kind.WARNING;

@ApiStatus.Experimental
public final class MessageBuilder {
  @Nullable private String myGroup;
  @NotNull private final String myTitle;
  @NotNull private final String myText;
  @Nullable private Exception myException;
  @NotNull private Kind myKind = INFO;
  @Nullable private String myFilePath;
  private int myLine;
  private int myColumn;

  private MessageBuilder(@NotNull String title, @NotNull String text) {
    myTitle = title;
    myText = text;
  }

  public static MessageBuilder create(@NotNull String title, @NotNull String text) {
    return new MessageBuilder(title, text);
  }

  public final MessageBuilder warning() {
    myKind = WARNING;
    return this;
  }

  public final MessageBuilder error() {
    myKind = Kind.ERROR;
    return this;
  }

  public final MessageBuilder withGroup(String group) {
    myGroup = group;
    return this;
  }

  public final MessageBuilder withException(Exception e) {
    myException = e;
    return this;
  }

  public MessageBuilder withLocation(String filePath, int line, int column) {
    myFilePath = filePath;
    myLine = line;
    myColumn = column;
    return this;
  }

  @NotNull
  public Message build() {
    String text = myText;
    if (myException != null) {
      if (myException.getStackTrace().length > 0) {
        text += ("\n\n" + getErrorMessage(myException));
      }
      else {
        text += ("\n\n" + myException.getMessage());
      }
    }
    FilePosition filePosition = myFilePath == null ? null : new FilePosition(myFilePath, myLine, myColumn);
    return new Message(myTitle, text, myGroup, myKind, filePosition);
  }

  public String buildJson() {
    return new GsonBuilder().create().toJson(build());
  }

  @Contract("null -> null; !null->!null")
  private static String getErrorMessage(@Nullable Throwable e) {
    if (e == null) return null;
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    ExternalSystemException esException = findCause(e, ExternalSystemException.class);
    if (esException != null && esException != e) {
      sw.append("\nCaused by: ").append(esException.getOriginalReason());
    }
    return sw.toString();
  }
}
