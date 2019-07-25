// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public abstract class ModulePackagingElementBase extends PackagingElement<ModulePackagingElementState> implements ModulePackagingElement {
  protected final Project myProject;
  protected ModulePointer myModulePointer;

  public ModulePackagingElementBase(PackagingElementType type, Project project, ModulePointer modulePointer) {
    super(type);
    myProject = project;
    myModulePointer = modulePointer;
  }

  public ModulePackagingElementBase(PackagingElementType type, Project project) {
    super(type);
    myProject = project;
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element.getClass() == getClass() && myModulePointer != null
           && myModulePointer.equals(((ModulePackagingElementBase)element).myModulePointer);
  }

  @Override
  public ModulePackagingElementState getState() {
    final ModulePackagingElementState state = new ModulePackagingElementState();
    if (myModulePointer != null) {
      state.setModuleName(myModulePointer.getModuleName());
    }
    return state;
  }

  @Override
  public void loadState(@NotNull ModulePackagingElementState state) {
    final String moduleName = state.getModuleName();
    myModulePointer = moduleName != null ? ModulePointerManager.getInstance(myProject).create(moduleName) : null;
  }

  @Override
  @Nullable
  public String getModuleName() {
    return myModulePointer != null ? myModulePointer.getModuleName() : null;
  }

  @Override
  @Nullable
  public Module findModule(PackagingElementResolvingContext context) {
    if (myModulePointer != null) {
      final Module module = myModulePointer.getModule();
      final ModulesProvider modulesProvider = context.getModulesProvider();
      if (module != null) {
        if (modulesProvider instanceof DefaultModulesProvider//optimization
            || ArrayUtil.contains(module, modulesProvider.getModules())) {
          return module;
        }
      }
      return modulesProvider.getModule(myModulePointer.getModuleName());
    }
    return null;
  }
}
