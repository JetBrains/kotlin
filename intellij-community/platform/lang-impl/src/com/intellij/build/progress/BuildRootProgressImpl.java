// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.progress;

import com.intellij.build.BuildProgressListener;
import com.intellij.build.events.FinishBuildEvent;
import com.intellij.build.events.FinishEvent;
import com.intellij.build.events.StartEvent;
import com.intellij.build.events.impl.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public class BuildRootProgressImpl extends BuildProgressImpl<BuildProgressDescriptor> {
  private BuildProgressListener myListener;

  public BuildRootProgressImpl(BuildProgressListener buildProgressListener) {
    super(buildProgressListener, null);
    myListener = buildProgressListener;
  }

  @NotNull
  @Override
  public Object getId() {
    return getBuildId();
  }

  @Override
  @NotNull
  protected StartEvent createStartEvent(BuildProgressDescriptor descriptor) {
    return new StartBuildEventImpl(descriptor.getBuildDescriptor(), "running...");
  }

  @NotNull
  @Override
  public BuildProgress<? extends BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate) {
    assertStarted();
    FinishEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, "finished", new SuccessResultImpl(isUpToDate));
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<BuildProgressDescriptor> fail() {
    return fail(System.currentTimeMillis(), "failed");
  }


  @NotNull
  @Override
  public BuildRootProgressImpl fail(long timeStamp, @NotNull String message) {
    assertStarted();
    FinishBuildEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new FailureResultImpl());
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<BuildProgressDescriptor> cancel() {
    return cancel(System.currentTimeMillis(), "cancelled");
  }

  @NotNull
  @Override
  public BuildRootProgressImpl cancel(long timeStamp, @NotNull String message) {
    assertStarted();
    FinishBuildEventImpl event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new SkippedResultImpl());
    myListener.onEvent(getBuildId(), event);
    return this;
  }
}
