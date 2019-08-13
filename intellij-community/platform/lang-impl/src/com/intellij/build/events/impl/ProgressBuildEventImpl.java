/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.build.events.impl;

import com.intellij.build.events.ProgressBuildEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class ProgressBuildEventImpl extends AbstractBuildEvent implements ProgressBuildEvent {

  private final long myTotal;
  private final long myProgress;
  private final String myUnit;

  public ProgressBuildEventImpl(@NotNull Object eventId,
                                @Nullable Object parentId,
                                long eventTime,
                                @NotNull String message,
                                long total,
                                long progress,
                                @NotNull String unit) {
    super(eventId, parentId, eventTime, message);
    myTotal = total;
    myProgress = progress;
    myUnit = unit;
  }

  @Override
  public long getTotal() {
    return myTotal;
  }

  @Override
  public long getProgress() {
    return myProgress;
  }

  @Override
  public String getUnit() {
    return myUnit;
  }
}
