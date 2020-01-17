// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.ui.DelegatedPackagingElementPresentation;
import com.intellij.packaging.impl.ui.ModuleElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.Collection;
import java.util.Collections;

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
