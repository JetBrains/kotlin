// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ProjectGeneratorPeer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
* @author Dmitry Avdeev
*/
public class WebModuleBuilder<T> extends ModuleBuilder {
  public static final String GROUP_NAME = "Static Web";
  public static final Icon ICON = AllIcons.Nodes.PpWeb;

  private final WebProjectTemplate<T> myTemplate;
  protected final NotNullLazyValue<ProjectGeneratorPeer<T>> myGeneratorPeerLazyValue;

  public WebModuleBuilder(@NotNull WebProjectTemplate<T> template) {
    myTemplate = template;
    myGeneratorPeerLazyValue = myTemplate.createLazyPeer();
  }

  public WebModuleBuilder() {
    myTemplate = null;
    myGeneratorPeerLazyValue = null;
  }

  @Override
  public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) throws ConfigurationException {
    doAddContentEntry(modifiableRootModel);
  }

  @Override
  public ModuleType getModuleType() {
    return WebModuleTypeBase.getInstance();
  }

  @Override
  public String getPresentableName() {
    return getGroupName();
  }

  @Override
  public boolean isTemplateBased() {
    return true;
  }

  @Override
  public String getGroupName() {
    return GROUP_NAME;
  }

  @Override
  public Icon getNodeIcon() {
    return myTemplate != null ? myTemplate.getIcon() : ICON;
  }

  @Nullable
  @Override
  public Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
    Module module = super.commitModule(project, model);
    if (module != null && myTemplate != null) {
      doGenerate(myTemplate, module);
    }
    return module;
  }

  private void doGenerate(@NotNull WebProjectTemplate<T> template, @NotNull Module module) {
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    VirtualFile[] contentRoots = moduleRootManager.getContentRoots();
    VirtualFile dir = module.getProject().getBaseDir();
    if (contentRoots.length > 0 && contentRoots[0] != null) {
      dir = contentRoots[0];
    }
    template.generateProject(module.getProject(), dir, myGeneratorPeerLazyValue.getValue().getSettings(), module);
  }

  @Nullable
  @Override
  public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
    if (myTemplate == null) {
      return super.modifySettingsStep(settingsStep);
    }
    myGeneratorPeerLazyValue.getValue().buildUI(settingsStep);
    return new ModuleWizardStep() {
      @Override
      public JComponent getComponent() {
        return null;
      }

      @Override
      public void updateDataModel() {
      }

      @Override
      public boolean validate() throws ConfigurationException {
        ValidationInfo info = myGeneratorPeerLazyValue.getValue().validate();
        if (info != null) throw new ConfigurationException(info.message);
        return true;
      }
    };
  }
}
