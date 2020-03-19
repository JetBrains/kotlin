// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.progress;

import com.intellij.build.FilePosition;
import com.intellij.build.events.MessageEvent;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface BuildProgress<T extends BuildProgressDescriptor> {
  @NotNull Object getId();

  @NotNull BuildProgress<T> start(@NotNull T descriptor);

  @NotNull BuildProgress<T> progress(@NotNull String title);

  @NotNull BuildProgress<T> output(@NotNull String text, boolean stdOut);

  @NotNull BuildProgress<T> message(@NotNull String title,
                                    @NotNull String message,
                                    @NotNull MessageEvent.Kind kind,
                                    @Nullable Navigatable navigatable);

  @NotNull BuildProgress<T> fileMessage(@NotNull String title,
                                        @NotNull String message,
                                        @NotNull MessageEvent.Kind kind,
                                        @NotNull FilePosition filePosition);

  @NotNull BuildProgress<? extends BuildProgressDescriptor> finish();

  @NotNull BuildProgress<? extends BuildProgressDescriptor> finish(long timeStamp);

  @NotNull BuildProgress<? extends BuildProgressDescriptor> finish(boolean isUpToDate);

  @NotNull BuildProgress<? extends BuildProgressDescriptor> finish(long timeStamp, boolean isUpToDate, @NotNull String message);

  @NotNull BuildProgress<? extends BuildProgressDescriptor> fail();

  @NotNull BuildProgress<? extends BuildProgressDescriptor> fail(long timeStamp, @NotNull String message);

  @NotNull BuildProgress<? extends BuildProgressDescriptor> cancel();

  @NotNull BuildProgress<? extends BuildProgressDescriptor> cancel(long timeStamp, @NotNull String message);

  @NotNull BuildProgress<? extends BuildProgressDescriptor> startChildProgress(@NotNull String title);
}
