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

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class TreeModel extends DefaultTreeModel {
  private int myMarkedFileCount;
  private int myTotalFileCount;

  public TreeModel(TreeNode root) {
    super(root);
  }

  public TreeModel(TreeNode root, int total, int marked) {
    super(root);
    myMarkedFileCount = marked;
    myTotalFileCount = total;
  }

  public void setMarkedFileCount(int markedFileCount) {
    myMarkedFileCount = markedFileCount;
  }

  public void setTotalFileCount(int totalFileCount) {
    myTotalFileCount = totalFileCount;
  }

  public int getMarkedFileCount() {
    return myMarkedFileCount;
  }

  public int getTotalFileCount() {
    return myTotalFileCount;
  }
}