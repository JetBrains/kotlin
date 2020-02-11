// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy.newAPI;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;

/**
 * @author cdr
 */
public final class ViewSupertypesHierarchyAction extends ChangeViewTypeActionBase {
  public ViewSupertypesHierarchyAction() {
    super(IdeBundle.lazyMessage("action.view.supertypes.hierarchy"),
          IdeBundle.lazyMessage("action.description.view.supertypes.hierarchy"), AllIcons.Hierarchy.Supertypes);
  }

  @Override
  protected final HierarchyScopeType getTypeName() {
    return TypeHierarchyBrowserBase.getSupertypesHierarchyType();
  }
}
