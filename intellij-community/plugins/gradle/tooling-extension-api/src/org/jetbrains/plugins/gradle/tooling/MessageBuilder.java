// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import org.gradle.internal.impldep.com.google.gson.GsonBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.util.ExceptionUtilRt.findCause;

@ApiStatus.Experimental
public final class MessageBuilder {
  @Nullable private String myGroup;
  @NotNull private final String myTitle;
  @NotNull private final String myText;
  @Nullable private Exception myException;
  @NotNull private Kind myKind = Kind.INFO;
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
    myKind = Kind.WARNING;
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

  public static class Message {
    @NotNull private final String myTitle;
    @NotNull private final String myText;
    @Nullable private final String myGroup;
    @NotNull private final Kind myKind;
    @Nullable FilePosition myFilePosition;

    public Message(@NotNull String title,
                   @NotNull String text,
                   @Nullable String group,
                   @NotNull Kind kind,
                   @Nullable FilePosition filePosition) {
      myTitle = title;
      myText = text;
      myGroup = group;
      myKind = kind;
      myFilePosition = filePosition;
    }

    @NotNull
    public String getTitle() {
      return myTitle;
    }

    @NotNull
    public String getText() {
      return myText;
    }

    @Nullable
    public String getGroup() {
      return myGroup;
    }

    @NotNull
    public Kind getKind() {
      return myKind;
    }

    @Nullable
    public FilePosition getFilePosition() {
      return myFilePosition;
    }
  }

  public static class FilePosition {
    @NotNull private final String myFilePath;
    private final int myLine;
    private final int myColumn;

    public FilePosition(@NotNull String filePath, int line, int column) {
      myFilePath = filePath;
      myLine = line;
      myColumn = column;
    }

    @NotNull
    public String getFilePath() {
      return myFilePath;
    }

    public int getLine() {
      return myLine;
    }

    public int getColumn() {
      return myColumn;
    }
  }

  public enum Kind {ERROR, WARNING, INFO}
}
