// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.frameworkSupport;

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder;

import javax.swing.*;

import static org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder.getBuildScriptData;

/**
 * @author Vladislav.Soroka
 */
public abstract class GradleFrameworkSupportProvider extends FrameworkSupportInModuleProvider {

  public static final ExtensionPointName<GradleFrameworkSupportProvider> EP_NAME =
    ExtensionPointName.create("org.jetbrains.plugins.gradle.frameworkSupport");

  /**
   * @deprecated use {@link #addSupport(ProjectId, Module, ModifiableRootModel, ModifiableModelsProvider, BuildScriptDataBuilder)}
   */
  @Deprecated
  public void addSupport(@NotNull Module module, @NotNull ModifiableRootModel rootModel,
                         @NotNull ModifiableModelsProvider modifiableModelsProvider,
                         @NotNull BuildScriptDataBuilder buildScriptData) {
  }

  public void addSupport(@NotNull ProjectId projectId,
                         @NotNull Module module,
                         @NotNull ModifiableRootModel rootModel,
                         @NotNull ModifiableModelsProvider modifiableModelsProvider,
                         @NotNull BuildScriptDataBuilder buildScriptData) {
    addSupport(module, rootModel, modifiableModelsProvider, buildScriptData);
  }

  public JComponent createComponent() {
    return null;
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
    return new FrameworkSupportInModuleConfigurable() {
      @Nullable
      @Override
      public JComponent createComponent() {
        return GradleFrameworkSupportProvider.this.createComponent();
      }

      @Override
      public void addSupport(@NotNull Module module,
                             @NotNull ModifiableRootModel rootModel,
                             @NotNull ModifiableModelsProvider modifiableModelsProvider) {
        final BuildScriptDataBuilder buildScriptData = getBuildScriptData(module);
        if (buildScriptData != null) {
          ModuleBuilder builder = model.getModuleBuilder();
          ProjectId projectId = builder instanceof AbstractGradleModuleBuilder ? ((AbstractGradleModuleBuilder)builder).getProjectId()
                                                                               : new ProjectId(null, module.getName(), null);
          GradleFrameworkSupportProvider.this.addSupport(projectId, module, rootModel, modifiableModelsProvider, buildScriptData);
        }
      }
    };
  }

  @Override
  public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
    return false;
  }
}
