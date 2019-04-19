/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ModuleStructureExtension;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class ExternalModuleStructureExtension extends ModuleStructureExtension {

  private static final Logger LOG = Logger.getInstance(ExternalModuleStructureExtension.class);
  @SuppressWarnings("StatefulEp")
  @Nullable
  private Project myProject;
  private boolean isExternalSystemsInvolved;
  @Nullable
  private Map<String, Pair<ProjectSystemId, ExternalProjectSettings>> myExternalProjectsToRestore;
  @Nullable
  private Map<String, ProjectSystemId> myOrphanProjectsCandidates;

  @Override
  public void reset(Project project) {
    if (project == null) return;
    myProject = project;
    isExternalSystemsInvolved = false;
    myExternalProjectsToRestore = getLinkedProjects(project);
    myOrphanProjectsCandidates = new HashMap<>();
  }

  @Override
  public void disposeUIResources() {
    try {
      if (isExternalSystemsInvolved) {
        assert myOrphanProjectsCandidates != null;
        assert myProject != null;
        if (myExternalProjectsToRestore != null) {
          for (Pair<ProjectSystemId, ExternalProjectSettings> settingsPair : myExternalProjectsToRestore.values()) {
            AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(myProject, settingsPair.first);
            String rootProjectPath = settingsPair.second.getExternalProjectPath();
            if (settings.getLinkedProjectSettings(rootProjectPath) == null) {
              //noinspection unchecked
              settings.linkProject(settingsPair.second);
            }
            myOrphanProjectsCandidates.remove(rootProjectPath);
          }
        }

        ModulesConfigurator modulesConfigurator = getModulesConfigurator(myProject);
        if (modulesConfigurator != null) {
          for (Map.Entry<String, ProjectSystemId> entry : myOrphanProjectsCandidates.entrySet()) {
            String rootProjectPath = entry.getKey();
            if (StringUtil.isNotEmpty(rootProjectPath)) {
              unlinkProject(myProject, entry.getValue(), rootProjectPath);
            }
          }
        }
      }
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
    finally {
      myProject = null;
      myExternalProjectsToRestore = null;
      myOrphanProjectsCandidates = null;
    }
  }

  @Override
  public boolean addModuleNodeChildren(Module module, MasterDetailsComponent.MyNode moduleNode, Runnable treeNodeNameUpdater) {
    String systemIdString = ExternalSystemModulePropertyManager.getInstance(module).getExternalSystemId();
    if (StringUtil.isNotEmpty(systemIdString)) {
      isExternalSystemsInvolved = true;
      String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
      if (myOrphanProjectsCandidates != null && StringUtil.isNotEmpty(rootProjectPath)) {
        myOrphanProjectsCandidates.put(rootProjectPath, new ProjectSystemId(systemIdString));
      }
    }
    return false;
  }

  @Override
  public void moduleRemoved(Module module) {
    String systemIdString = ExternalSystemModulePropertyManager.getInstance(module).getExternalSystemId();
    if (StringUtil.isEmpty(systemIdString)) return;

    String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    if (StringUtil.isEmpty(rootProjectPath)) return;

    Project project = module.getProject();
    ModulesConfigurator modulesConfigurator = getModulesConfigurator(project);
    if (modulesConfigurator == null) return;

    for (Module m : modulesConfigurator.getModules()) {
      if (m != module && rootProjectPath.equals(ExternalSystemApiUtil.getExternalRootProjectPath(m))) {
        return;
      }
    }

    ProjectSystemId systemId = new ProjectSystemId(systemIdString);
    ExternalSystemApiUtil.getSettings(project, systemId).unlinkExternalProject(rootProjectPath);
    assert myOrphanProjectsCandidates != null;
    myOrphanProjectsCandidates.put(rootProjectPath, systemId);
    isExternalSystemsInvolved = true;
  }

  @Override
  public void afterModelCommit() {
    if (myProject == null) return;
    myExternalProjectsToRestore = getLinkedProjects(myProject);
  }

  @Nullable
  private static ModulesConfigurator getModulesConfigurator(Project project) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) return null;
    final ProjectStructureConfigurable structureConfigurable = ProjectStructureConfigurable.getInstance(project);
    StructureConfigurableContext context = structureConfigurable.isUiInitialized() ? structureConfigurable.getContext() : null;
    return context != null ? context.getModulesConfigurator() : null;
  }

  private static Map<String, Pair<ProjectSystemId, ExternalProjectSettings>> getLinkedProjects(Project project) {
    Map<String, Pair<ProjectSystemId, ExternalProjectSettings>> result = new HashMap<>();
    for (ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
      ProjectSystemId systemId = manager.getSystemId();
      AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(project, systemId);
      Collection projectsSettings = systemSettings.getLinkedProjectsSettings();
      for (Object settings : projectsSettings) {
        if (settings instanceof ExternalProjectSettings) {
          ExternalProjectSettings projectSettings = (ExternalProjectSettings)settings;
          result.put((projectSettings).getExternalProjectPath(), Pair.create(systemId, projectSettings));
        }
      }
    }

    return result;
  }

  private static void unlinkProject(@NotNull Project project, ProjectSystemId systemId, String rootProjectPath) {
    ExternalSystemApiUtil.getLocalSettings(project, systemId).forgetExternalProjects(Collections.singleton(rootProjectPath));
    ExternalSystemApiUtil.getSettings(project, systemId).unlinkExternalProject(rootProjectPath);
    ExternalProjectsManagerImpl.getInstance(project).forgetExternalProjectData(systemId, rootProjectPath);
  }
}
