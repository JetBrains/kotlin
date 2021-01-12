// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.progress;

import com.intellij.build.BuildBundle;
import com.intellij.build.BuildProgressListener;
import com.intellij.build.events.BuildEventsNls;
import com.intellij.build.events.FinishBuildEvent;
import com.intellij.build.events.FinishEvent;
import com.intellij.build.events.StartEvent;
import com.intellij.build.events.impl.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public class BuildRootProgressImpl extends BuildProgressImpl {
  private final BuildProgressListener myListener;

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
    return new StartBuildEventImpl(descriptor.getBuildDescriptor(), BuildBundle.message("build.status.running"));
  }

  @Override
  public @NotNull BuildProgress<BuildProgressDescriptor> finish() {
    return finish(System.currentTimeMillis(), false, BuildBundle.message("build.status.finished"));
  }

  @NotNull
  @Override
  public BuildProgress<BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, @NotNull @BuildEventsNls.Message String message) {
    assertStarted();
    FinishEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new SuccessResultImpl(isUpToDate));
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<BuildProgressDescriptor> fail() {
    return fail(System.currentTimeMillis(), BuildBundle.message("build.status.failed"));
  }

  @NotNull
  @Override
  public BuildRootProgressImpl fail(long timeStamp, @NotNull @BuildEventsNls.Message String message) {
    assertStarted();
    FinishBuildEvent event = new FinishBuildEventImpl(getId(), null, timeStamp, message, new FailureResultImpl());
    myListener.onEvent(getBuildId(), event);
    return this;
  }

  @NotNull
  @Override
  public BuildProgress<BuildProgressDescriptor> cancel() {
    return cancel(System.currentTimeMillis(), BuildBundle.message("build.status.cancelled"));
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
