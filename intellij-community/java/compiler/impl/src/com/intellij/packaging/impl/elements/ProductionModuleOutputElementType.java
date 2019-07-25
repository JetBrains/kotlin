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
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
* @author nik
*/
public class ProductionModuleOutputElementType extends ModuleOutputElementTypeBase<ProductionModuleOutputPackagingElement> {
  public static final ProductionModuleOutputElementType ELEMENT_TYPE = new ProductionModuleOutputElementType();

  private ProductionModuleOutputElementType() {
    super("module-output", CompilerBundle.message("element.type.name.module.output"));
  }

  @Override
  @NotNull
  public ProductionModuleOutputPackagingElement createEmpty(@NotNull Project project) {
    return new ProductionModuleOutputPackagingElement(project);
  }

  @Override
  protected ModuleOutputPackagingElementBase createElement(@NotNull Project project, @NotNull ModulePointer pointer) {
    return new ProductionModuleOutputPackagingElement(project, pointer);
  }

  @Override
  public Icon getCreateElementIcon() {
    return AllIcons.Nodes.Module;
  }

  @NotNull
  @Override
  public String getElementText(@NotNull String moduleName) {
    return CompilerBundle.message("node.text.0.compile.output", moduleName);
  }

  @Override
  public boolean isSuitableModule(@NotNull ModulesProvider modulesProvider, @NotNull Module module) {
    return modulesProvider.getRootModel(module).getSourceRootUrls(false).length > 0;
  }
}
