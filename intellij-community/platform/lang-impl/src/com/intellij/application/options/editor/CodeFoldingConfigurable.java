// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.editor;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.extensions.BaseExtensionPointName;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.ConfigurableBuilder;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author yole
 */
public class CodeFoldingConfigurable extends CompositeConfigurable<CodeFoldingOptionsProvider> implements EditorOptionsProvider,
                                                                                                          ConfigurableWrapper.WithEpDependencies {
  public static final String ID = "editor.preferences.folding";

  private JCheckBox myCbFolding;
  private JPanel myRootPanel;
  private JPanel myFoldingPanel;

  @Override
  @Nls
  public String getDisplayName() {
    return ApplicationBundle.message("group.code.folding");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.code.folding";
  }

  @Override
  public JComponent createComponent() {
    myFoldingPanel.removeAll();
    JBIterable<CodeFoldingOptionsProvider> providers = JBIterable.from(getConfigurables())
      .sort(Comparator.comparing(CodeFoldingConfigurable::sortByTitle));
    for (CodeFoldingOptionsProvider provider : providers) {
      JComponent component = provider.createComponent();
      assert component != null : "CodeFoldingOptionsProvider " + provider.getClass() + " has a null component.";
      myFoldingPanel.add(component, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                                                           GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0));
    }
    return myRootPanel;
  }

  @Override
  public boolean isModified() {
    return myCbFolding.isSelected() != EditorSettingsExternalizable.getInstance().isFoldingOutlineShown() ||
           super.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable.getInstance().setFoldingOutlineShown(myCbFolding.isSelected());
    super.apply();

    ApplicationManager.getApplication().invokeLater(() -> applyCodeFoldingSettingsChanges(), ModalityState.NON_MODAL);
  }

  public static void applyCodeFoldingSettingsChanges() {
    EditorOptionsPanel.reinitAllEditors();
    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      Project project = editor.getProject();
      if (project != null && !project.isDefault()) CodeFoldingManager.getInstance(project).scheduleAsyncFoldingUpdate(editor);
    }
    ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorOptionsListener.FOLDING_CONFIGURABLE_TOPIC).changesApplied();
  }

  @Override
  public void reset() {
    myCbFolding.setSelected(EditorSettingsExternalizable.getInstance().isFoldingOutlineShown());
    super.reset();
  }

  @NotNull
  @Override
  protected List<CodeFoldingOptionsProvider> createConfigurables() {
    return ConfigurableWrapper.createConfigurables(CodeFoldingOptionsProviderEP.EP_NAME);
  }

  @NotNull
  @Override
  public Collection<BaseExtensionPointName<?>> getDependencies() {
    return Collections.singleton(CodeFoldingOptionsProviderEP.EP_NAME);
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }

  @NotNull
  private static String sortByTitle(@NotNull CodeFoldingOptionsProvider p) {
    String title = ConfigurableBuilder.getConfigurableTitle(p);
    if (ApplicationBundle.message("title.general").equals(title)) return "";
    return title != null ? title : "z";
  }
}
