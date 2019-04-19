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
package com.intellij.build;

import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class DefaultBuildDescriptor implements BuildDescriptor {

  private final Object myId;
  private final String myTitle;
  private final String myWorkingDir;
  private final long myStartTime;

  public DefaultBuildDescriptor(@NotNull Object id, @NotNull String title, @NotNull String workingDir, long startTime) {
    myId = id;
    myTitle = title;
    myWorkingDir = workingDir;
    myStartTime = startTime;
  }


  @NotNull
  @Override
  public Object getId() {
    return myId;
  }

  @NotNull
  @Override
  public String getTitle() {
    return myTitle;
  }

  @NotNull
  @Override
  public String getWorkingDir() {
    return myWorkingDir;
  }

  @Override
  public long getStartTime() {
    return myStartTime;
  }
}
