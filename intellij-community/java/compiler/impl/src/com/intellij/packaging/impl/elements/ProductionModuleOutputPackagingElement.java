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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.ui.DelegatedPackagingElementPresentation;
import com.intellij.packaging.impl.ui.ModuleElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.Collection;
import java.util.Collections;

/**
 * @author nik
 */
public class ProductionModuleOutputPackagingElement extends ModuleOutputPackagingElementBase {
  public ProductionModuleOutputPackagingElement(@NotNull Project project) {
    super(ProductionModuleOutputElementType.ELEMENT_TYPE, project);
  }

  public ProductionModuleOutputPackagingElement(@NotNull Project project, @NotNull ModulePointer modulePointer) {
    super(ProductionModuleOutputElementType.ELEMENT_TYPE, project, modulePointer);
  }

  @NonNls @Override
  public String toString() {
    return "module:" + getModuleName();
  }

  @Override
  protected String getDirectoryAntProperty(ArtifactAntGenerationContext generationContext) {
    return generationContext.getModuleOutputPath(myModulePointer.getModuleName());
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getSourceRoots(PackagingElementResolvingContext context) {
    Module module = findModule(context);
    if (module == null) return Collections.emptyList();

    ModuleRootModel rootModel = context.getModulesProvider().getRootModel(module);
    return rootModel.getSourceRoots(JavaModuleSourceRootTypes.PRODUCTION);
  }

  @Override
  @NotNull
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DelegatedPackagingElementPresentation(new ModuleElementPresentation(myModulePointer, context, ProductionModuleOutputElementType.ELEMENT_TYPE));
  }
}
