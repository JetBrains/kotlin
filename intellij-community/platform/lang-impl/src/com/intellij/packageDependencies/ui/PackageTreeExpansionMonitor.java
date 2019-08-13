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

package com.intellij.packageDependencies.ui;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiManager;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

public class PackageTreeExpansionMonitor {
  private PackageTreeExpansionMonitor() {
  }

  public static TreeExpansionMonitor<PackageDependenciesNode> install(final JTree tree, final Project project) {
    return new TreeExpansionMonitor<PackageDependenciesNode>(tree) {
      @Override
      protected TreePath findPathByNode(final PackageDependenciesNode node) {
         if (node.getPsiElement() == null){
           return new TreePath(node.getPath());
         }
          PsiManager manager = PsiManager.getInstance(project);
          Enumeration enumeration = ((DefaultMutableTreeNode)tree.getModel().getRoot()).breadthFirstEnumeration();
          while (enumeration.hasMoreElements()) {
            final Object nextElement = enumeration.nextElement();
            if (nextElement instanceof PackageDependenciesNode) { //do not include root
              PackageDependenciesNode child = (PackageDependenciesNode)nextElement;
              if (manager.areElementsEquivalent(child.getPsiElement(), node.getPsiElement())) {
                return new TreePath(child.getPath());
              }
            }
          }
          return null;
      }
    };
  }
}