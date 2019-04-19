// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events.impl;

import com.intellij.build.FileNavigatable;
import com.intellij.build.FilePosition;
import com.intellij.build.events.FileMessageEvent;
import com.intellij.build.events.FileMessageEventResult;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Vladislav.Soroka
 */
public class FileMessageEventImpl extends MessageEventImpl implements FileMessageEvent {

  private final FilePosition myFilePosition;

  public FileMessageEventImpl(@NotNull Object parentId,
                              @NotNull Kind kind,
                              @Nullable String group,
                              @NotNull String message,
                              @Nullable String detailedMessage,
                              @NotNull FilePosition filePosition) {
    super(parentId, kind, group, message, detailedMessage);
    myFilePosition = filePosition;
  }

  @Override
  public FileMessageEventResult getResult() {
    return new FileMessageEventResult() {
      @Override
      public FilePosition getFilePosition() {
        return myFilePosition;
      }

      @Override
      public Kind getKind() {
        return FileMessageEventImpl.this.getKind();
      }

      @Override
      @Nullable
      public String getDetails() {
        return getDescription();
      }
    };
  }

  @Override
  public FilePosition getFilePosition() {
    return myFilePosition;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(@NotNull Project project) {
    return new FileNavigatable(project, myFilePosition);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    FileMessageEventImpl event = (FileMessageEventImpl)o;
    return Objects.equals(myFilePosition, event.myFilePosition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), myFilePosition);
  }
}
