/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ModuleGroup {
  public static final DataKey<ModuleGroup[]> ARRAY_DATA_KEY = DataKey.create("moduleGroup.array");
  private final List<String> myGroupPath;

  public ModuleGroup(@NotNull List<String> groupPath) {
    myGroupPath = groupPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleGroup)) return false;

    return myGroupPath.equals(((ModuleGroup)o).myGroupPath);
  }

  @Override
  public int hashCode() {
    return myGroupPath.hashCode();
  }

  @NotNull
  public String[] getGroupPath() {
    return ArrayUtil.toStringArray(myGroupPath);
  }

  @NotNull
  public List<String> getGroupPathList() {
    return myGroupPath;
  }

  @NotNull
  public Collection<Module> modulesInGroup(@NotNull Project project, boolean recursively) {
    return modulesInGroup(ModuleGrouper.instanceFor(project), recursively);
  }

  @NotNull
  public Collection<Module> modulesInGroup(@NotNull ModuleGrouper grouper, boolean recursively) {
    List<Module> result = new ArrayList<>();
    Set<List<String>> moduleAsGroupsPaths = ContainerUtil.map2Set(grouper.getAllModules(), module -> grouper.getModuleAsGroupPath(module));
    for (final Module module : grouper.getAllModules()) {
      List<String> group = grouper.getGroupPath(module);
      if (myGroupPath.equals(group) || isChild(myGroupPath, group) && (recursively || isUnderGroupWithSameNameAsSomeModule(myGroupPath, group, moduleAsGroupsPaths))) {
        result.add(module);
      }
    }
    return result;
  }

  private static boolean isUnderGroupWithSameNameAsSomeModule(@NotNull List<String> parent, @NotNull List<String> descendant, @NotNull Set<List<String>> moduleNamesAsGroups) {
    return descendant.size() > parent.size() && moduleNamesAsGroups.contains(descendant.subList(0, parent.size() + 1));
  }

  @NotNull
  public Collection<ModuleGroup> childGroups(@NotNull ModuleGrouper grouper) {
    Set<ModuleGroup> result = new THashSet<>();
    Set<List<String>> moduleAsGroupsPaths = ContainerUtil.map2Set(grouper.getAllModules(), module -> grouper.getModuleAsGroupPath(module));
    for (Module module : grouper.getAllModules()) {
      List<String> group = grouper.getGroupPath(module);
      if (isChild(myGroupPath, group)) {
        final List<String> directChild = ContainerUtil.append(myGroupPath, group.get(myGroupPath.size()));
        if (!moduleAsGroupsPaths.contains(directChild)) {
          result.add(new ModuleGroup(directChild));
        }
      }
    }

    return result;
  }

  private static boolean isChild(@NotNull List<String> parent, @NotNull List<String> descendant) {
    return descendant.size() > parent.size() && descendant.subList(0, parent.size()).equals(parent);
  }

  @NotNull
  public String presentableText() {
    return "'" + myGroupPath.get(myGroupPath.size() - 1) + "'";
  }

  @NotNull
  public String getQualifiedName() {
    return StringUtil.join(myGroupPath, ".");
  }

  @Override
  public String toString() {
    return myGroupPath.get(myGroupPath.size() - 1);
  }
}
