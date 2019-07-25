// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.roots.IconActionComponent;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public abstract class JavaSourceRootEditHandlerBase extends ModuleSourceRootEditHandler<JavaSourceRootProperties> {
  public JavaSourceRootEditHandlerBase(JpsModuleSourceRootType<JavaSourceRootProperties> rootType) {
    super(rootType);
  }

  @NotNull
  @Override
  public Icon getRootIcon(@NotNull JavaSourceRootProperties properties) {
    return properties.isForGeneratedSources() ? getGeneratedRootIcon() : getRootIcon();
  }

  @Nullable
  @Override
  public Icon getRootFileLayerIcon(@NotNull JavaSourceRootProperties properties) {
    return AllIcons.Modules.SourceRootFileLayer;
  }

  @NotNull
  protected abstract Icon getGeneratedRootIcon();

  @Nullable
  @Override
  public String getPropertiesString(@NotNull JavaSourceRootProperties properties) {
    StringBuilder buffer = new StringBuilder();
    if (properties.isForGeneratedSources()) {
      buffer.append(" [generated]");
    }
    String packagePrefix = properties.getPackagePrefix();
    if (!packagePrefix.isEmpty()) {
      buffer.append(" (").append(packagePrefix).append(")");
    }
    return buffer.length() > 0 ? buffer.toString() : null;
  }

  @Nullable
  @Override
  public JComponent createPropertiesEditor(@NotNull final SourceFolder folder,
                                           @NotNull final JComponent parentComponent,
                                           @NotNull final ContentRootPanel.ActionCallback callback) {
    final IconActionComponent iconComponent = new IconActionComponent(AllIcons.Modules.SetPackagePrefix,
                                                                      AllIcons.Modules.SetPackagePrefixRollover,
                                                                      ProjectBundle.message("module.paths.edit.properties.tooltip"), () -> {
                                                                        JavaSourceRootProperties properties = folder.getJpsElement().getProperties(JavaModuleSourceRootTypes.SOURCES);
                                                                        assert properties != null;
                                                                        SourceRootPropertiesDialog dialog = new SourceRootPropertiesDialog(parentComponent, properties);
                                                                        if (dialog.showAndGet()) {
                                                                          callback.onSourceRootPropertiesChanged(folder);
                                                                        }
                                                                      });
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.add(iconComponent, BorderLayout.CENTER);
    panel.add(Box.createHorizontalStrut(3), BorderLayout.EAST);
    return panel;
  }

  private static class SourceRootPropertiesDialog extends DialogWrapper {
    private final JTextField myPackagePrefixField;
    private final JCheckBox myIsGeneratedCheckBox;
    private final JPanel myMainPanel;
    @NotNull private final JavaSourceRootProperties myProperties;

    private SourceRootPropertiesDialog(@NotNull JComponent parentComponent, @NotNull JavaSourceRootProperties properties) {
      super(parentComponent, true);
      myProperties = properties;
      setTitle(ProjectBundle.message("module.paths.edit.properties.title"));
      myPackagePrefixField = new JTextField();
      myIsGeneratedCheckBox = new JCheckBox(UIUtil.replaceMnemonicAmpersand("For &generated sources"));
      myMainPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Package &prefix:", myPackagePrefixField)
        .addComponent(myIsGeneratedCheckBox)
        .getPanel();
      myPackagePrefixField.setText(myProperties.getPackagePrefix());
      myPackagePrefixField.setColumns(25);
      myIsGeneratedCheckBox.setSelected(myProperties.isForGeneratedSources());
      init();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
      return myPackagePrefixField;
    }

    @Override
    protected void doOKAction() {
      myProperties.setPackagePrefix(myPackagePrefixField.getText().trim());
      myProperties.setForGeneratedSources(myIsGeneratedCheckBox.isSelected());
      super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return myMainPanel;
    }
  }
}
