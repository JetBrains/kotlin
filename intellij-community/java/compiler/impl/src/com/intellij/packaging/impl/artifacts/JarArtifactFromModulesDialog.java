// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class JarArtifactFromModulesDialog extends DialogWrapper {
  private JPanel myMainPanel;
  private TextFieldWithBrowseButton myMainClassField;
  private JComboBox<Module> myModuleComboBox;
  private JLabel myMainClassLabel;
  private TextFieldWithBrowseButton myManifestDirField;
  private JLabel myManifestDirLabel;
  private JRadioButton myExtractJarsRadioButton;
  private JRadioButton myCopyJarsRadioButton;
  private JCheckBox myIncludeTestsCheckBox;
  private final PackagingElementResolvingContext myContext;

  public JarArtifactFromModulesDialog(PackagingElementResolvingContext context) {
    super(context.getProject());
    myContext = context;
    setTitle(JavaCompilerBundle.message("create.jar.from.modules"));
    myMainClassLabel.setLabelFor(myMainClassField.getTextField());
    myManifestDirLabel.setLabelFor(myManifestDirField.getTextField());

    final Project project = myContext.getProject();
    ManifestFileUtil.setupMainClassField(project, myMainClassField);
    myMainClassField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        updateManifestDirField();
      }
    });
    final ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateManifestDirField();
      }
    };
    myExtractJarsRadioButton.addActionListener(actionListener);
    myCopyJarsRadioButton.addActionListener(actionListener);

    updateManifestDirField();
    myManifestDirField.addBrowseFolderListener(null, null, project, ManifestFileUtil.createDescriptorForManifestDirectory());

    setupModulesCombobox(context);
    init();
  }

  private void setupModulesCombobox(PackagingElementResolvingContext context) {
    final Module[] modules = context.getModulesProvider().getModules().clone();
    Arrays.sort(modules, ModulesAlphaComparator.INSTANCE);
    if (modules.length > 1) {
      myModuleComboBox.addItem(null);
    }
    for (Module module : modules) {
      myModuleComboBox.addItem(module);
    }
    myModuleComboBox.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      label.setIcon(value != null ? ModuleType.get(value).getIcon() : null);
      label.setText(value != null ? value.getName() : JavaCompilerBundle.message("all.modules"));
    }));
    new ComboboxSpeedSearch(myModuleComboBox) {
      @Override
      protected String getElementText(Object element) {
        return element instanceof Module ? ((Module)element).getName() : "";
      }
    };
  }

  private void updateManifestDirField() {
    final boolean enable = !myMainClassField.getText().isEmpty() || !myExtractJarsRadioButton.isSelected();
    setManifestDirFieldEnabled(enable);
    if (enable && myManifestDirField.getText().isEmpty()) {
      final VirtualFile file = ManifestFileUtil.suggestManifestFileDirectory(myContext.getProject(), getSelectedModule());
      if (file != null) {
        myManifestDirField.setText(FileUtil.toSystemDependentName(file.getPath()));
      }
    }
  }

  @Nullable
  private Module getSelectedModule() {
    return (Module)myModuleComboBox.getSelectedItem();
  }

  public Module @NotNull [] getSelectedModules() {
    final Module module = getSelectedModule();
    if (module != null) {
      return new Module[]{module};
    }
    return myContext.getModulesProvider().getModules();
  }

  @NotNull
  public String getDirectoryForManifest() {
    return FileUtil.toSystemIndependentName(myManifestDirField.getText());
  }

  public boolean isExtractLibrariesToJar() {
    return myExtractJarsRadioButton.isSelected();
  }

  public boolean isIncludeTests() {
    return myIncludeTestsCheckBox.isSelected();
  }

  public String getMainClassName() {
    return myMainClassField.getText();
  }

  private void setManifestDirFieldEnabled(boolean enabled) {
    myManifestDirLabel.setEnabled(enabled);
    myManifestDirField.setEnabled(enabled);
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  @Override
  protected String getHelpId() {
    return "reference.project.structure.artifacts.jar.from.module";
  }
}
