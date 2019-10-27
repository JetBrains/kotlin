// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data;

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractModuleDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.BUILTIN_MODULE_DATA_SERVICE_ORDER + 1)
public class GradleSourceSetDataService extends AbstractModuleDataService<GradleSourceSetData> {

  @NotNull
  @Override
  public Key<GradleSourceSetData> getTargetDataKey() {
    return GradleSourceSetData.KEY;
  }

  @NotNull
  @Override
  public Computable<Collection<Module>> computeOrphanData(@NotNull final Collection<DataNode<GradleSourceSetData>> toImport,
                                                          @NotNull final ProjectData projectData,
                                                          @NotNull final Project project,
                                                          @NotNull final IdeModifiableModelsProvider modelsProvider) {
    return () -> {
      List<Module> orphanIdeModules = new SmartList<>();

      for (Module module : modelsProvider.getModules()) {
        if (module.isDisposed()) continue;
        if (!ExternalSystemApiUtil.isExternalSystemAwareModule(projectData.getOwner(), module)) continue;
        if (!GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY.equals(ExternalSystemApiUtil.getExternalModuleType(module))) continue;

        final String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
        if (projectData.getLinkedExternalProjectPath().equals(rootProjectPath)) {
          if (module.getUserData(AbstractModuleDataService.MODULE_DATA_KEY) == null) {
            orphanIdeModules.add(module);
          }
        }
      }

      return orphanIdeModules;
    };
  }

  @Override
  protected void setModuleOptions(Module module, DataNode<GradleSourceSetData> moduleDataNode) {
    super.setModuleOptions(module, moduleDataNode);
    ExternalSystemModulePropertyManager.getInstance(module).setExternalModuleType(GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY);
  }
}
