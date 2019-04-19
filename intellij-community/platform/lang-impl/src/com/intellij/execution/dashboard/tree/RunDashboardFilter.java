// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.tree;

import com.intellij.ide.util.treeView.AbstractTreeNode;

/**
 * @author Konstantin Aleev
 */
public interface RunDashboardFilter {
  boolean isVisible(AbstractTreeNode<?> node);
}
