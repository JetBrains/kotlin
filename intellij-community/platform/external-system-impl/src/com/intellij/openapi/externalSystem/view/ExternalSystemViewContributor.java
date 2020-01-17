/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.view;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public abstract class ExternalSystemViewContributor {
  public static final ExtensionPointName<ExternalSystemViewContributor> EP_NAME =
    ExtensionPointName.create("com.intellij.externalSystemViewContributor");

  @NotNull
  public abstract ProjectSystemId getSystemId();

  @NotNull
  public abstract List<Key<?>> getKeys();

  @NotNull
  public abstract List<ExternalSystemNode<?>> createNodes(
    ExternalProjectsView externalProjectsView, MultiMap<Key<?>, DataNode<?>> dataNodes);

  @Nullable
  public String getDisplayName(@NotNull DataNode node) {
    return null;
  }

  public ExternalProjectsStructure.ErrorLevel getErrorLevel(DataNode<?> dataNode) {
    return ExternalProjectsStructure.ErrorLevel.NONE;
  }
}
