// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.progress;

import com.intellij.build.BuildDescriptor;
import com.intellij.build.BuildProgressListener;
import com.intellij.build.FilePosition;
import com.intellij.build.events.EventResult;
import com.intellij.build.events.FinishEvent;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.events.StartEvent;
import com.intellij.build.events.impl.*;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
class BuildProgressImpl<T extends BuildProgressDescriptor> implements BuildProgress<T> {
  private Object myId = new Object();
  private BuildProgressListener myListener;
  @Nullable
  private BuildProgress<? extends BuildProgressDescriptor> myParentProgress;
  private T myDescriptor;

  BuildProgressImpl(BuildProgressListener listener, @Nullable BuildProgress<? extends BuildProgressDescriptor> parentProgress) {
    myListener = listener;
    myParentProgress = parentProgress;
  }

  protected Object getBuildId() {
    return myDescriptor.getBuildDescriptor().getId();
  }

  @NotNull
  @Override
  public Object getId() {
    return myId;
  }

  @NotNull
  @Override
  public BuildProgress<T> start(@NotNull T descriptor) {
    myDescriptor = descriptor;
    StartEvent event = createStartEvent(descriptor);
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @NotNull
  protected StartEvent createStartEvent(T descriptor) {
    assert myParentProgress != null;
    return new StartEventImpl(getId(), myParentProgress.getId(), System.currentTimeMillis(), descriptor.getTitle());
  }

  @NotNull
  @Override
  public BuildProgress<BuildProgressDescriptor> startChildProgress(@NotNull String title) {
    BuildDescriptor buildDescriptor = myDescriptor.getBuildDescriptor();
    return new BuildProgressImpl<>(myListener, this).start(new BuildProgressDescriptor() {

      @NotNull
      @Override
      public String getTitle() {
        return title;
      }

      @Override
      public @NotNull BuildDescriptor getBuildDescriptor() {
        return buildDescriptor;
      }
    });
  }

  @NotNull
  @Override
  public BuildProgress<T> progress(@NotNull String title) {
    Object parentId = myParentProgress != null ? myParentProgress.getId() : null;
    myListener.onEvent(getBuildId(), new ProgressBuildEventImpl(getId(), parentId, System.currentTimeMillis(), title, -1, -1, ""));
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<T> output(@NotNull String text, boolean stdOut) {
    myListener.onEvent(getBuildId(), new OutputBuildEventImpl(getId(), text, stdOut));
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<T> fileMessage(@NotNull String title,
                                      @NotNull String message,
                                      @NotNull MessageEvent.Kind kind,
                                      @NotNull FilePosition filePosition) {
    StringBuilder fileLink = new StringBuilder(filePosition.getFile().getPath());
    if (filePosition.getStartLine() > 0) {
      fileLink.append(":").append(filePosition.getStartLine() + 1);
      if (filePosition.getStartColumn() > 0) {
        fileLink.append(":").append(filePosition.getStartColumn() + 1);
      }
    }
    String detailedMessage = fileLink.toString() + '\n' + message;
    FileMessageEventImpl event = new FileMessageEventImpl(getId(), kind, null, title, detailedMessage, filePosition);
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<T> message(@NotNull String title,
                                  @NotNull String message,
                                  @NotNull MessageEvent.Kind kind,
                                  @Nullable Navigatable navigatable) {
    MessageEventImpl event = new MessageEventImpl(getId(), kind, null, title, message) {
      @Override
      public @Nullable Navigatable getNavigatable(@NotNull Project project) {
        return navigatable;
      }
    };
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> finish() {
    return finish(false);
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> finish(boolean isUpToDate) {
    return finish(System.currentTimeMillis(), isUpToDate, myDescriptor.getTitle());
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> finish(long timeStamp) {
    return finish(timeStamp, false, myDescriptor.getTitle());
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, @NotNull String message) {
    assertStarted();
    assert myParentProgress != null;
    EventResult result = new SuccessResultImpl(isUpToDate);
    FinishEvent event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, result);
    myListener.onEvent(getBuildId(), event);
    return myParentProgress;
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> fail() {
    return fail(System.currentTimeMillis(), myDescriptor.getTitle());
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> fail(long timeStamp, @NotNull String message) {
    assertStarted();
    assert myParentProgress != null;
    FinishEvent event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, new FailureResultImpl());
    myListener.onEvent(getBuildId(), event);
    return myParentProgress;
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> cancel() {
    return cancel(System.currentTimeMillis(), myDescriptor.getTitle());
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> cancel(long timeStamp, @NotNull String message) {
    assertStarted();
    assert myParentProgress != null;
    FinishEventImpl event = new FinishEventImpl(getId(), myParentProgress.getId(), timeStamp, message, new SkippedResultImpl());
    myListener.onEvent(getBuildId(), event);
    return myParentProgress;
  }

  protected void assertStarted() {
    if (myDescriptor == null) {
      //noinspection HardCodedStringLiteral
      throw new IllegalStateException("The start event was not triggered yet.");
    }
  }
}
