// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events.impl;

import com.intellij.build.events.MessageEvent;
import com.intellij.build.events.MessageEventResult;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Vladislav.Soroka
 */
public class MessageEventImpl extends AbstractBuildEvent implements MessageEvent {

  @NotNull private final Kind myKind;
  @NotNull private final String myGroup;

  public MessageEventImpl(@NotNull Object parentId, @NotNull Kind kind, @Nullable String group, @NotNull String message, @Nullable String detailedMessage) {
    super(new Object(), parentId, System.currentTimeMillis(), message);
    myKind = kind;
    myGroup = group == null ? "Other messages" : group;
    setDescription(detailedMessage);
  }

  @Override
  public final void setDescription(@Nullable String description) {
    super.setDescription(description);
  }

  @NotNull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @NotNull
  @Override
  public String getGroup() {
    return myGroup;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(@NotNull Project project) {
    return null;
  }

  @Override
  public MessageEventResult getResult() {
    return new MessageEventResult() {
      @Override
      public Kind getKind() {
        return myKind;
      }

      @Override
      public String getDetails() {
        return getDescription();
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MessageEventImpl event = (MessageEventImpl)o;
    return Objects.equals(getMessage(), event.getMessage()) &&
           Objects.equals(getDescription(), event.getDescription()) &&
           Objects.equals(myGroup, event.myGroup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myGroup, getMessage());
  }
}
