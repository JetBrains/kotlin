// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;

public class TestModuleOutputElementType extends ModuleOutputElementTypeBase<TestModuleOutputPackagingElement> {
  public static final TestModuleOutputElementType ELEMENT_TYPE = new TestModuleOutputElementType();

  private TestModuleOutputElementType() {
    super("module-test-output", JavaCompilerBundle.message("element.type.name.module.test.output"));
  }

  @NotNull
  @Override
  public TestModuleOutputPackagingElement createEmpty(@NotNull Project project) {
    return new TestModuleOutputPackagingElement(project);
  }

  @Override
  protected ModuleOutputPackagingElementBase createElement(@NotNull Project project, @NotNull ModulePointer pointer) {
    return new TestModuleOutputPackagingElement(project, pointer);
  }

  @Override
  public Icon getCreateElementIcon() {
    return AllIcons.Nodes.TestSourceFolder;
  }

  @Override
  public Icon getElementIcon(@Nullable Module module) {
    return AllIcons.Nodes.TestSourceFolder;
  }

  @NotNull
  @Override
  public String getElementText(@NotNull String moduleName) {
    return JavaCompilerBundle.message("node.text.0.test.compile.output", moduleName);
  }

  @Override
  public boolean isSuitableModule(@NotNull ModulesProvider modulesProvider, @NotNull Module module) {
    return !modulesProvider.getRootModel(module).getSourceRoots(JavaModuleSourceRootTypes.TESTS).isEmpty();
  }
}
