// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleGrouper
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap

/**
 * Provides a cache which can be used to quickly get subgroups and child modules of any module group.
 */
internal class ModuleGroupsTree private constructor(private val grouper: ModuleGrouper) {
  private val childGroups = MultiMap.createSet<ModuleGroup, ModuleGroup>()
  private val childModules = MultiMap.create<ModuleGroup, Module>()

  init {
    val moduleAsGroupPaths = grouper.getAllModules().mapNotNullTo(HashSet()) { grouper.getModuleAsGroupPath(it) }
    for (module in grouper.getAllModules()) {
      val groupPath = grouper.getGroupPath(module)
      if (groupPath.isNotEmpty()) {
        val group = ModuleGroup(groupPath)
        val moduleNamePrefixLen = (1 .. groupPath.size).firstOrNull { groupPath.subList(0, it) in moduleAsGroupPaths }
        val parentGroupForModule = if (moduleNamePrefixLen != null && moduleNamePrefixLen > 1) {
          //if there are modules with names 'a.foo' and 'a.foo.bar.baz' the both should be shown as children of module group 'a' to avoid
          // nodes with same text in the tree
          ModuleGroup(groupPath.subList(0, moduleNamePrefixLen - 1))
        }
        else {
          group
        }
        childModules.putValue(parentGroupForModule, module)
        var parentGroupPath = groupPath
        while (parentGroupPath.size > 1 && parentGroupPath !in moduleAsGroupPaths) {
          val nextParentGroupPath = parentGroupPath.subList(0, parentGroupPath.size - 1)
          childGroups.putValue(ModuleGroup(nextParentGroupPath), ModuleGroup(parentGroupPath))
          parentGroupPath = nextParentGroupPath
        }
      }
    }
  }

  fun getChildGroups(group: ModuleGroup): Collection<ModuleGroup> = childGroups[group]

  fun getModulesInGroup(group: ModuleGroup): Collection<Module> = childModules[group]

  companion object {
    private val key = Key.create<CachedValue<ModuleGroupsTree>>("MODULE_GROUPS_TREE")

    @JvmStatic
    fun getModuleGroupTree(project: Project): ModuleGroupsTree {
      return CachedValuesManager.getManager(project).getCachedValue(project, key, {
        val tree = ModuleGroupsTree(ModuleGrouper.instanceFor(project))
        CachedValueProvider.Result.createSingleDependency(tree, ProjectRootManager.getInstance(project))
      }, false)
    }
  }
}