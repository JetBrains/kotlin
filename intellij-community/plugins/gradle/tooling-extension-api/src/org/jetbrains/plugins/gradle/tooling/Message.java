// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public class Message {
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
