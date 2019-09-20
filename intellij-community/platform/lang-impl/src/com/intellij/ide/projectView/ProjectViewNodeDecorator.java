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

package com.intellij.ide.projectView;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;

/**
 * Allows to modify the presentation of project view and package dependencies view nodes.
 *
 * @author yole
 * @see com.intellij.ide.projectView.TreeStructureProvider
 */
public interface ProjectViewNodeDecorator {
  ExtensionPointName<ProjectViewNodeDecorator> EP_NAME = ExtensionPointName.create("com.intellij.projectViewNodeDecorator");

  /**
   * Modifies the presentation of a project view node.
   *
   * @param node the node to modify (use {@link ProjectViewNode#getValue()} to get the object represented by the node).
   * @param data the current presentation of the node, which you can modify as necessary.
   */
  void decorate(ProjectViewNode node, PresentationData data);

  /**
   * Modifies the presentation of a package dependencies view node.
   *
   * @param node the node to modify.
   * @param cellRenderer the current renderer for the node, which you can modify as necessary.
   */
  void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer);
}
