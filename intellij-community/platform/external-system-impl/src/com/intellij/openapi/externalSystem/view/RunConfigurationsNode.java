/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.view;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import icons.ExternalSystemIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemNode.BUILTIN_RUN_CONFIGURATIONS_DATA_NODE_ORDER)
public class RunConfigurationsNode extends ExternalSystemNode<Void> {

  private final ModuleData myModuleData;

  public RunConfigurationsNode(@NotNull ExternalProjectsView externalProjectsView, ModuleNode parent) {
    super(externalProjectsView, parent, null);
    myModuleData = parent.getData();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(ExternalSystemIcons.TaskGroup);
  }

  @Override
  public String getName() {
    return message("external.system.view.nodes.run_configurations.name");
  }

  @Override
  public boolean isVisible() {
    return super.isVisible() && hasChildren();
  }

  @NotNull
  @Override
  protected List<? extends ExternalSystemNode> doBuildChildren() {
    List<ExternalSystemNode> runConfigurationNodes = ContainerUtil.newArrayList();
    final AbstractExternalSystemTaskConfigurationType configurationType = ExternalSystemUtil.findConfigurationType(myModuleData.getOwner());
    if (configurationType == null) return Collections.emptyList();

    Set<RunnerAndConfigurationSettings> settings = new THashSet<>(
      RunManager.getInstance(myProject).getConfigurationSettingsList(configurationType));


    String directory = PathUtil.getCanonicalPath(myModuleData.getLinkedExternalProjectPath());

    for (RunnerAndConfigurationSettings cfg : settings) {
      ExternalSystemRunConfiguration externalSystemRunConfiguration = (ExternalSystemRunConfiguration)cfg.getConfiguration();

      if (directory.equals(PathUtil.getCanonicalPath(externalSystemRunConfiguration.getSettings().getExternalProjectPath()))) {
        runConfigurationNodes.add(new RunConfigurationNode(getExternalProjectsView(), this, cfg));
      }
    }

    return runConfigurationNodes;
  }

  public void updateRunConfigurations() {
    cleanUpCache();
    getExternalProjectsView().updateUpTo(this);
  }
}
