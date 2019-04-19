/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.ui;

import com.intellij.packaging.artifacts.Artifact;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Override this class to provide additional nodes in 'Available Elements' tree on 'Output Layout' tab of an artifact editor. This tree
 * contains elements which are usually included into an artifact so don't include optional and rarely used items there. All packaging elements
 * may be added by clicking on '+' icon on the toolbar above 'Output Layout' tree anyway.
 *
 * <p/>
 * The implementation should be registered in plugin.xml file:
 * <pre>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;
 * &nbsp;&nbsp;&lt;packaging.sourceItemProvider implementation="qualified-class-name"/&gt;
 * &lt;/extensions&gt;
 * </pre>
 */
public abstract class PackagingSourceItemsProvider {
  public static final ExtensionPointName<PackagingSourceItemsProvider> EP_NAME = ExtensionPointName.create("com.intellij.packaging.sourceItemProvider");

  /**
   * Return items which should be shown be shown under {@code parent} node in 'Available Elements' tree for {@code artifact}.
   */
  @NotNull
  public abstract Collection<? extends PackagingSourceItem> getSourceItems(@NotNull ArtifactEditorContext editorContext, @NotNull Artifact artifact,
                                                                           @Nullable PackagingSourceItem parent);
}
