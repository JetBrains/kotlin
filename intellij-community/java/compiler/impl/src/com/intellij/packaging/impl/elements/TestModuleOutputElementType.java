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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;

/**
 * @author nik
 */
public class TestModuleOutputElementType extends ModuleOutputElementTypeBase<TestModuleOutputPackagingElement> {
  public static final TestModuleOutputElementType ELEMENT_TYPE = new TestModuleOutputElementType();

  private TestModuleOutputElementType() {
    super("module-test-output", CompilerBundle.message("element.type.name.module.test.output"));
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
    return AllIcons.Modules.TestSourceFolder;
  }

  @Override
  public Icon getElementIcon(@Nullable Module module) {
    return AllIcons.Modules.TestSourceFolder;
  }

  @NotNull
  @Override
  public String getElementText(@NotNull String moduleName) {
    return CompilerBundle.message("node.text.0.test.compile.output", moduleName);
  }

  @Override
  public boolean isSuitableModule(@NotNull ModulesProvider modulesProvider, @NotNull Module module) {
    return !modulesProvider.getRootModel(module).getSourceRoots(JavaModuleSourceRootTypes.TESTS).isEmpty();
  }
}
