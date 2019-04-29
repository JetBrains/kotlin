/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.compiler.generic;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated this class is part of the old deprecated build infrastructure; plug into the external build process instead (see {@link org.jetbrains.jps.builders.BuildTarget})
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "192.0")
public abstract class CompileItem<Key, SourceState, OutputState> {
  @NotNull
  public abstract Key getKey();

  public abstract boolean isSourceUpToDate(@NotNull SourceState state);

  @NotNull
  public abstract SourceState computeSourceState();


  public abstract boolean isOutputUpToDate(@NotNull OutputState state);

  @NotNull
  public abstract OutputState computeOutputState();

  public boolean isExcluded() {
    return false;
  }
}
