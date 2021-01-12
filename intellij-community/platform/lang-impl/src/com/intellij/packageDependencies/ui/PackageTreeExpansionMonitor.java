// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.packageDependencies.ui;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

public final class PackageTreeExpansionMonitor {
  private PackageTreeExpansionMonitor() {
  }

  public static TreeExpansionMonitor<PackageDependenciesNode> install(final JTree tree, final Project project) {
    return new TreeExpansionMonitor<PackageDependenciesNode>(tree) {
      @Override
      protected TreePath findPathByNode(final PackageDependenciesNode node) {
         if (node.getPsiElement() == null){
           return new TreePath(node.getPath());
         }
          Enumeration enumeration = ((DefaultMutableTreeNode)tree.getModel().getRoot()).breadthFirstEnumeration();
          while (enumeration.hasMoreElements()) {
            final Object nextElement = enumeration.nextElement();
            if (nextElement instanceof PackageDependenciesNode) { //do not include root
              PackageDependenciesNode child = (PackageDependenciesNode)nextElement;
              if (child.equals(node)) {
                return new TreePath(child.getPath());
              }
            }
          }
          return null;
      }
    };
  }
}