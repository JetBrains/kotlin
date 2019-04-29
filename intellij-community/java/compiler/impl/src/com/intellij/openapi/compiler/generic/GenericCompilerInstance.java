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

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * @deprecated this class is part of the old deprecated build infrastructure; plug into the external build process instead
 * (see {@link org.jetbrains.jps.incremental.TargetBuilder})
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "192.0")
public abstract class GenericCompilerInstance<T extends BuildTarget, Item extends CompileItem<Key, SourceState, OutputState>, Key, SourceState, OutputState> {
  protected final CompileContext myContext;

  protected GenericCompilerInstance(CompileContext context) {
    myContext = context;
  }

  protected Project getProject() {
    return myContext.getProject();
  }

  @NotNull
  public abstract List<T> getAllTargets();

  @NotNull
  public abstract List<T> getSelectedTargets();

  public abstract void processObsoleteTarget(@NotNull String targetId, @NotNull List<GenericCompilerCacheState<Key, SourceState, OutputState>> obsoleteItems);


  @NotNull
  public abstract List<Item> getItems(@NotNull T target);

  public abstract void processItems(@NotNull T target, @NotNull List<? extends GenericCompilerProcessingItem<Item, SourceState, OutputState>> changedItems, @NotNull List<GenericCompilerCacheState<Key, SourceState, OutputState>> obsoleteItems,
                                    @NotNull OutputConsumer<? super Item> consumer);

  public interface OutputConsumer<Item extends CompileItem<?,?,?>> {
    void addFileToRefresh(@NotNull File file);

    void addDirectoryToRefresh(@NotNull File dir);

    void addProcessedItem(@NotNull Item sourceItem);
  }
}
