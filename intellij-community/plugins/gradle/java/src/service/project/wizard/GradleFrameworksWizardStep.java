// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard;

import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.newProjectWizard.AddSupportForFrameworksPanel;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportNodeBase;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportModelBase;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.StatisticsAwareModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider;
import org.jetbrains.plugins.gradle.frameworkSupport.KotlinDslGradleFrameworkSupportProvider;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class GradleFrameworksWizardStep extends ModuleWizardStep implements Disposable, StatisticsAwareModuleWizardStep {

  private JPanel myPanel;
  private final AddSupportForFrameworksPanel myFrameworksPanel;
  private JPanel myFrameworksPanelPlaceholder;
  private JPanel myOptionsPanel;
  @SuppressWarnings("unused") private JBLabel myFrameworksLabel;
  private JCheckBox kdslCheckBox;

  public GradleFrameworksWizardStep(WizardContext context, final GradleModuleBuilder builder) {

    Project project = context.getProject();
    final LibrariesContainer container = LibrariesContainerFactory.createContainer(context.getProject());
    FrameworkSupportModelBase model = new FrameworkSupportModelBase(project, builder, container) {
      @NotNull
      @Override
      public String getBaseDirectoryForLibrariesPath() {
        return StringUtil.notNullize(builder.getContentEntryPath());
      }
    };

    myFrameworksPanel =
      new AddSupportForFrameworksPanel(Collections.emptyList(), model, true, null);

    setGradleFrameworkSupportProviders(Collections.singleton(
      "java"/*KotlinDslGradleJavaFrameworkSupportProvider.ID*/));

    Disposer.register(this, myFrameworksPanel);
    myFrameworksPanelPlaceholder.add(myFrameworksPanel.getMainPanel());

    ModuleBuilder.ModuleConfigurationUpdater configurationUpdater = new ModuleBuilder.ModuleConfigurationUpdater() {
      @Override
      public void update(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
        myFrameworksPanel.addSupport(module, rootModel);
      }
    };
    builder.addModuleConfigurationUpdater(configurationUpdater);

    ((CardLayout)myOptionsPanel.getLayout()).show(myOptionsPanel, "frameworks card");

    kdslCheckBox.addActionListener((actionEvent) -> {
      builder.setUseKotlinDsl(kdslCheckBox.isSelected());

      Set<String> selectedNodeIds = ContainerUtil.map2Set(myFrameworksPanel.getSelectedNodes(), FrameworkSupportNodeBase::getId);
      if (kdslCheckBox.isSelected()) {
        setKotlinDslGradleFrameworkSupportProviders(selectedNodeIds);
      } else {
        setGradleFrameworkSupportProviders(selectedNodeIds);
      }
    });
  }

  private void setKotlinDslGradleFrameworkSupportProviders(Set<String> selectedNodeIds) {
    List<FrameworkSupportInModuleProvider> providers = new ArrayList<>();
    Collections.addAll(providers, KotlinDslGradleFrameworkSupportProvider.EP_NAME.getExtensions());
    myFrameworksPanel.setProviders(providers, Collections.emptySet(), selectedNodeIds);
  }

  private void setGradleFrameworkSupportProviders(Set<String> selectedNodeIds) {
    List<FrameworkSupportInModuleProvider> providers = new ArrayList<>();
    Collections.addAll(providers, GradleFrameworkSupportProvider.EP_NAME.getExtensions());
    myFrameworksPanel.setProviders(providers, Collections.emptySet(), selectedNodeIds);
  }

  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public void updateDataModel() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(this);
  }

  @Override
  public void addCustomFeatureUsageData(@NotNull String eventId, @NotNull FeatureUsageData data) {
    myFrameworksPanel.reportSelectedFrameworks(eventId, data);
    data.addData("gradle-kotlin-dsl", kdslCheckBox.isSelected());
  }
}
