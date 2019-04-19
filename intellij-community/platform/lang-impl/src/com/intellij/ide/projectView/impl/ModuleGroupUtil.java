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

package com.intellij.ide.projectView.impl;

import com.intellij.util.Consumer;
import com.intellij.util.Function;

import java.util.List;
import java.util.Map;

public class ModuleGroupUtil {
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
