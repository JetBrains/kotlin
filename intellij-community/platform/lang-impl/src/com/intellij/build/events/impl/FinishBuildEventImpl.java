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

import com.intellij.build.events.EventResult;
import com.intellij.build.events.FinishBuildEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class FinishBuildEventImpl extends FinishEventImpl implements FinishBuildEvent {

  public FinishBuildEventImpl(@NotNull Object eventId,
                              @Nullable Object parentId,
                              long eventTime,
                              @NotNull String message,
                              @NotNull EventResult result) {
    super(eventId, parentId, eventTime, message, result);
  }
}
