// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.io.FileUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
    presentation.setIcon(AllIcons.Nodes.ConfigFolder);
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
  protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
    List<ExternalSystemNode<?>> runConfigurationNodes = new ArrayList<>();
    final AbstractExternalSystemTaskConfigurationType configurationType = ExternalSystemUtil.findConfigurationType(myModuleData.getOwner());
    if (configurationType == null) return Collections.emptyList();

    Set<RunnerAndConfigurationSettings> settings = new THashSet<>(
      RunManager.getInstance(myProject).getConfigurationSettingsList(configurationType));


    String directory = FileUtil.toCanonicalPath(myModuleData.getLinkedExternalProjectPath());

    for (RunnerAndConfigurationSettings cfg : settings) {
      ExternalSystemRunConfiguration externalSystemRunConfiguration = (ExternalSystemRunConfiguration)cfg.getConfiguration();

      if (directory.equals(FileUtil.toCanonicalPath(externalSystemRunConfiguration.getSettings().getExternalProjectPath()))) {
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
