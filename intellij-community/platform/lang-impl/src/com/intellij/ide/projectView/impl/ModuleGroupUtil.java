// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.util.Consumer;
import com.intellij.util.Function;

import java.util.List;
import java.util.Map;

public final class ModuleGroupUtil {
  private ModuleGroupUtil() {
  }

  public static <T> T buildModuleGroupPath(final ModuleGroup group,
                                           T parentNode,
                                           final Map<ModuleGroup, T> map,
                                           final Consumer<? super ParentChildRelation<T>> insertNode,
                                           final Function<? super ModuleGroup, ? extends T> createNewNode) {
    final List<String> groupPath = group.getGroupPathList();
    for (int i = 0; i < groupPath.size(); i++) {
      final ModuleGroup moduleGroup = new ModuleGroup(groupPath.subList(0, i+1));
      T moduleGroupNode = map.get(moduleGroup);
      if (moduleGroupNode == null) {
        moduleGroupNode = createNewNode.fun(moduleGroup);
        map.put(moduleGroup, moduleGroupNode);
        insertNode.consume(new ParentChildRelation<>(parentNode, moduleGroupNode));
      }
      parentNode = moduleGroupNode;
    }
    return parentNode;
  }

  public static <T> T updateModuleGroupPath(final ModuleGroup group,
                                            T parentNode,
                                            final Function<? super ModuleGroup, ? extends T> needToCreateNode,
                                            final Consumer<? super ParentChildRelation<T>> insertNode,
                                            final Function<? super ModuleGroup, ? extends T> createNewNode) {
    final List<String> groupPath = group.getGroupPathList();
    for (int i = 0; i < groupPath.size(); i++) {
      final ModuleGroup moduleGroup = new ModuleGroup(groupPath.subList(0, i+1));
      T moduleGroupNode = needToCreateNode.fun(moduleGroup);
      if (moduleGroupNode == null) {
        moduleGroupNode = createNewNode.fun(moduleGroup);
        insertNode.consume(new ParentChildRelation<>(parentNode, moduleGroupNode));
      }
      parentNode = moduleGroupNode;
    }
    return parentNode;
  }

  public static class ParentChildRelation<T> {
    private final T myParent;
    private final T myChild;

    ParentChildRelation(final T parent, final T child) {
      myParent = parent;
      myChild = child;
    }


    public T getParent() {
      return myParent;
    }

    public T getChild() {
      return myChild;
    }
  }
}
