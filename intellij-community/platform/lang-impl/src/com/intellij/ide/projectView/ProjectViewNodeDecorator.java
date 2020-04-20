// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.ProjectExtensionPointName;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.ui.ColoredTreeCellRenderer;

/**
 * Allows to modify the presentation of project view and package dependencies view nodes.
 *
 * @author yole
 * @see TreeStructureProvider
 */
public interface ProjectViewNodeDecorator {
  ProjectExtensionPointName<ProjectViewNodeDecorator> EP = new ProjectExtensionPointName<>("com.intellij.projectViewNodeDecorator");

  /**
   * @deprecated Use {@link #EP}
   */
  @Deprecated
  ExtensionPointName<ProjectViewNodeDecorator> EP_NAME = ExtensionPointName.create("com.intellij.projectViewNodeDecorator");

  /**
   * Modifies the presentation of a project view node.
   *
   * @param node the node to modify (use {@link ProjectViewNode#getValue()} to get the object represented by the node).
   * @param data the current presentation of the node, which you can modify as necessary.
   */
  void decorate(ProjectViewNode<?> node, PresentationData data);

  /**
   * Modifies the presentation of a package dependencies view node.
   *
   * @param node the node to modify.
   * @param cellRenderer the current renderer for the node, which you can modify as necessary.
   */
  void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer);
}
