// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.impl.RootConfigurationAccessor
import com.intellij.testFramework.assertions.Assertions
import org.assertj.core.api.Assertions.assertThat


internal fun checkModuleRootModelConsistency(rootModel: ModuleRootModel) {
  assertThat(rootModel.contentRoots).containsExactly(*rootModel.contentEntries.mapNotNull { it.file }.toTypedArray())
  assertThat(rootModel.contentRootUrls).containsExactly(*rootModel.contentEntries.map { it.url }.toTypedArray())
  assertThat(rootModel.excludeRoots).containsExactly(
    *rootModel.contentEntries.flatMap { it.excludeFolderFiles.asIterable() }.toTypedArray())
  assertThat(rootModel.excludeRootUrls).containsExactly(
    *rootModel.contentEntries.flatMap { it.excludeFolderUrls.asIterable() }.toTypedArray())

  val allSourceRoots = rootModel.contentEntries.flatMap { it.sourceFolderFiles.asIterable() }
  assertThat(rootModel.sourceRoots).containsExactly(*allSourceRoots.toTypedArray())
  assertThat(rootModel.getSourceRoots(true)).containsExactly(*allSourceRoots.toTypedArray())
  assertThat(rootModel.getSourceRoots(false)).containsExactly(*rootModel.contentEntries.flatMap { contentEntry ->
    contentEntry.sourceFolders.filter { !it.isTestSource }.mapNotNull { it.file }.asIterable()
  }.toTypedArray())

  val allSourceRootUrls = rootModel.contentEntries.flatMap { contentEntry -> contentEntry.sourceFolders.map { it.url }.asIterable() }
  assertThat(rootModel.sourceRootUrls).containsExactly(*allSourceRootUrls.toTypedArray())
  assertThat(rootModel.getSourceRootUrls(true)).containsExactly(*allSourceRootUrls.toTypedArray())
  assertThat(rootModel.getSourceRootUrls(false)).containsExactly(*rootModel.contentEntries.flatMap { contentEntry ->
    contentEntry.sourceFolders.filter { !it.isTestSource }.map { it.url }.asIterable()
  }.toTypedArray())

  assertThat(rootModel.dependencyModuleNames).containsExactly(
    *rootModel.orderEntries.filterIsInstance(ModuleOrderEntry::class.java).map {
      it.moduleName
    }.toTypedArray())
  val moduleDependencies = rootModel.orderEntries.filterIsInstance(ModuleOrderEntry::class.java).mapNotNull { it.module }.toTypedArray()
  assertThat(rootModel.moduleDependencies).containsExactly(*moduleDependencies)
  assertThat(rootModel.getModuleDependencies(true)).containsExactly(*moduleDependencies)
  assertThat(rootModel.getModuleDependencies(false)).containsExactly(
    *rootModel.orderEntries.filterIsInstance(
      ModuleOrderEntry::class.java).filter { it.scope != DependencyScope.TEST }.mapNotNull { it.module }.toTypedArray()
  )
}

internal fun checkContentEntryConsistency(entry: ContentEntry) {
  assertThat(entry.sourceFolderFiles).containsExactly(*entry.sourceFolders.mapNotNull { it.file }.toTypedArray())
  assertThat(entry.excludeFolderFiles).containsExactly(*entry.excludeFolders.mapNotNull { it.file }.toTypedArray())
  assertThat(entry.excludeFolderUrls).containsExactly(*entry.excludeFolders.map { it.url }.toTypedArray())
}

internal fun commitModifiableRootModel(model: ModifiableRootModel, assertChanged: Boolean = true): ModuleRootManager {
  assertThat(model.isChanged).isEqualTo(assertChanged)
  checkModuleRootModelConsistency(model)
  runWriteActionAndWait { model.commit() }
  val moduleRootManager = ModuleRootManager.getInstance(model.module)
  checkModuleRootModelConsistency(moduleRootManager)
  return moduleRootManager
}

internal fun createModifiableModel(module: Module,
                                   accessor: RootConfigurationAccessor = RootConfigurationAccessor()): ModifiableRootModel {
  return runReadAction {
    val moduleRootManager = ModuleRootManagerEx.getInstanceEx(module)
    checkModuleRootModelConsistency(moduleRootManager)
    val model = moduleRootManager.getModifiableModel(accessor)
    checkModuleRootModelConsistency(model)
    model
  }
}

internal fun dropModuleSourceEntry(model: ModuleRootModel, additionalEntries: Int): List<OrderEntry> {
  val orderEntries = model.orderEntries
  Assertions.assertThat(orderEntries).hasSize(additionalEntries + 1)
  model.orderEntries.filterIsInstance<ModuleSourceOrderEntry>().single()
  return model.orderEntries.filter { it !is ModuleSourceOrderEntry }
}
