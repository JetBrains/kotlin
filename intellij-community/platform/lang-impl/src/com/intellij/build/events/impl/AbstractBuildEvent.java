// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events.impl;

import com.intellij.build.events.BuildEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractBuildEvent implements BuildEvent {

  @NotNull
  private final Object myEventId;
  @Nullable
  private final Object myParentId;
  private final long myEventTime;
  @NotNull
  private final String myMessage;
  @Nullable
  private String myHint;
  @Nullable
  private String myDescription;

  public AbstractBuildEvent(@NotNull Object eventId, @Nullable Object parentId, long eventTime, @NotNull String message) {
    myEventId = eventId;
    myParentId = parentId;
    myEventTime = eventTime;
    myMessage = message;
  }

  @NotNull
  @Override
  public Object getId() {
    return myEventId;
  }

  @Nullable
  @Override
  public Object getParentId() {
    return myParentId;
  }

  @Override
  public long getEventTime() {
    return myEventTime;
  }

  @NotNull
  @Override
  public String getMessage() {
    return myMessage;
  }

  @Override
  @Nullable
  public String getHint() {
    return myHint;
  }

  public void setHint(@Nullable String hint) {
    myHint = hint;
  }

  @Nullable
  @Override
  public String getDescription() {
    return myDescription;
  }

  public void setDescription(@Nullable String description) {
    myDescription = description;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append("{");
    sb.append("myEventId=").append(myEventId);
    sb.append(", myParentId=").append(myParentId);
    sb.append(", myEventTime=").append(myEventTime);
    sb.append(", myMessage='").append(myMessage).append('\'');
    sb.append(", myHint='").append(myHint).append('\'');
    sb.append(", myDescription='").append(myDescription).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
