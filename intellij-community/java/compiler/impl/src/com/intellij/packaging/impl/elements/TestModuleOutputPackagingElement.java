/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.ui.DelegatedPackagingElementPresentation;
import com.intellij.packaging.impl.ui.ModuleElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.Collection;
import java.util.Collections;

/**
 * @author nik
 */
public class TestModuleOutputPackagingElement extends ModuleOutputPackagingElementBase {
  public TestModuleOutputPackagingElement(Project project) {
    super(TestModuleOutputElementType.ELEMENT_TYPE, project);
  }

  public TestModuleOutputPackagingElement(Project project, ModulePointer modulePointer) {
    super(TestModuleOutputElementType.ELEMENT_TYPE, project, modulePointer);
  }

  @Override
  public String toString() {
    return "module-tests:" + getModuleName();
  }

  @Override
  protected String getDirectoryAntProperty(ArtifactAntGenerationContext generationContext) {
    return generationContext.getModuleTestOutputPath(myModulePointer.getModuleName());
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getSourceRoots(PackagingElementResolvingContext context) {
    Module module = findModule(context);
    if (module == null) return Collections.emptyList();

    return context.getModulesProvider().getRootModel(module).getSourceRoots(JavaModuleSourceRootTypes.TESTS);
  }

  @Override
  @NotNull
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DelegatedPackagingElementPresentation(new ModuleElementPresentation(myModulePointer, context, TestModuleOutputElementType.ELEMENT_TYPE));
  }
}
