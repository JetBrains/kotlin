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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.ui.ArtifactEditorContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public abstract class ModuleElementTypeBase<E extends ModulePackagingElementBase> extends PackagingElementType<E> {
  public ModuleElementTypeBase(String id, String presentableName) {
    super(id, presentableName);
  }

  @Override
  public boolean canCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
    return !getSuitableModules(context).isEmpty();
  }

  @Override
  @NotNull
  public List<? extends PackagingElement<?>> chooseAndCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact,
                                                                       @NotNull CompositePackagingElement<?> parent) {
    List<Module> suitableModules = getSuitableModules(context);
    List<Module> selected = context.chooseModules(suitableModules, ProjectBundle.message("dialog.title.packaging.choose.module"));

    final List<PackagingElement<?>> elements = new ArrayList<>();
    final ModulePointerManager pointerManager = ModulePointerManager.getInstance(context.getProject());
    for (Module module : selected) {
      elements.add(createElement(context.getProject(), pointerManager.create(module)));
    }
    return elements;
  }

  protected abstract ModulePackagingElementBase createElement(@NotNull Project project, @NotNull ModulePointer pointer);

  private List<Module> getSuitableModules(ArtifactEditorContext context) {
    ModulesProvider modulesProvider = context.getModulesProvider();
    ArrayList<Module> modules = new ArrayList<>();
    for (Module module : modulesProvider.getModules()) {
      if (isSuitableModule(modulesProvider, module)) {
        modules.add(module);
      }
    }
    return modules;
  }

  public abstract boolean isSuitableModule(@NotNull ModulesProvider modulesProvider, @NotNull Module module);

  /**
   * Provides element presentation text.
   * @param moduleName name of the module for which this presentation is requested.
   * @return text to display.
   */
  @NotNull
  public abstract String getElementText(@NotNull String moduleName);

  public Icon getElementIcon(@Nullable Module module) {
    return module != null ? ModuleType.get(module).getIcon() : AllIcons.Modules.Output;
  }
}
