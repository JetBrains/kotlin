// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements

import com.intellij.icons.AllIcons
import com.intellij.openapi.compiler.CompilerBundle
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModulePointer
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import javax.swing.Icon

class ProductionModuleSourceElementType private constructor() : ModuleElementTypeBase<ProductionModuleSourcePackagingElement>(
  "module-source", CompilerBundle.message("element.type.name.module.source")) {

  override fun isSuitableModule(modulesProvider: ModulesProvider, module: Module): Boolean {
    return modulesProvider.getRootModel(module).getSourceRootUrls(false).isNotEmpty()
  }

  override fun createElement(project: Project, pointer: ModulePointer) = ProductionModuleSourcePackagingElement(project, pointer)
  override fun createEmpty(project: Project) = ProductionModuleSourcePackagingElement(project)
  override fun getCreateElementIcon(): Icon = AllIcons.Modules.SourceFolder
  override fun getElementIcon(module: Module?): Icon = AllIcons.Modules.SourceFolder
  override fun getElementText(moduleName: String) = CompilerBundle.message("node.text.0.module.sources", moduleName)

  companion object {
    @JvmField
    val ELEMENT_TYPE = ProductionModuleSourceElementType()
  }
}
