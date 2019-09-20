// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.ui.properties;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.impl.elements.CompositeElementWithManifest;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.ManifestFileConfiguration;
import com.intellij.packaging.ui.PackagingElementPropertiesPanel;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author nik
 */
public abstract class ElementWithManifestPropertiesPanel<E extends CompositeElementWithManifest<?>> extends PackagingElementPropertiesPanel {
  private final E myElement;
  private final ArtifactEditorContext myContext;
  private JPanel myMainPanel;
  private TextFieldWithBrowseButton myMainClassField;
  private TextFieldWithBrowseButton myClasspathField;
  private JLabel myTitleLabel;
  private JButton myCreateManifestButton;
  private JButton myUseExistingManifestButton;
  private JPanel myPropertiesPanel;
  private JTextField myManifestPathField;
  private JLabel myManifestNotFoundLabel;
  private ManifestFileConfiguration myManifestFileConfiguration;

  public ElementWithManifestPropertiesPanel(E element, final ArtifactEditorContext context) {
    myElement = element;
    myContext = context;

    ManifestFileUtil.setupMainClassField(context.getProject(), myMainClassField);

    myClasspathField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Messages.showTextAreaDialog(myClasspathField.getTextField(), "Edit Classpath", "classpath-attribute-editor");
      }
    });
    myClasspathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myContext.queueValidation();
      }
    });
    myUseExistingManifestButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        chooseManifest();
      }
    });
    myCreateManifestButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        createManifest();
      }
    });
  }

  private void createManifest() {
    final VirtualFile file = ManifestFileUtil.showDialogAndCreateManifest(myContext, myElement);
    if (file == null) {
      return;
    }

    ManifestFileUtil.addManifestFileToLayout(file.getPath(), myContext, myElement);
    updateManifest();
    myContext.getThisArtifactEditor().updateLayoutTree();
  }

  private void chooseManifest() {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        return super.isFileVisible(file, showHiddenFiles) && (file.isDirectory() ||
               file.getName().equalsIgnoreCase(ManifestFileUtil.MANIFEST_FILE_NAME));
      }
    };
    descriptor.setTitle("Specify Path to MANIFEST.MF File");
    final VirtualFile file = FileChooser.chooseFile(descriptor, myContext.getProject(), null);
    if (file == null) return;

    ManifestFileUtil.addManifestFileToLayout(file.getPath(), myContext, myElement);
    updateManifest();
    myContext.getThisArtifactEditor().updateLayoutTree();
  }

  private void updateManifest() {
    myManifestFileConfiguration = myContext.getManifestFile(myElement, myContext.getArtifactType());
    final String card;
    if (myManifestFileConfiguration != null) {
      card = "properties";
      myManifestPathField.setText(FileUtil.toSystemDependentName(myManifestFileConfiguration.getManifestFilePath()));
      myMainClassField.setText(StringUtil.notNullize(myManifestFileConfiguration.getMainClass()));
      myMainClassField.setEnabled(myManifestFileConfiguration.isWritable());
      myClasspathField.setText(StringUtil.join(myManifestFileConfiguration.getClasspath(), " "));
      myClasspathField.setEnabled(myManifestFileConfiguration.isWritable());
    }
    else {
      card = "buttons";
      myManifestPathField.setText("");
    }
    ((CardLayout)myPropertiesPanel.getLayout()).show(myPropertiesPanel, card);
  }

  @Override
  public void reset() {
    myTitleLabel.setText("'" + myElement.getName() + "' manifest properties:");
    myManifestNotFoundLabel.setText("META-INF/MANIFEST.MF file not found in '" + myElement.getName() + "'");
    updateManifest();
  }

  @Override
  public boolean isModified() {
    return myManifestFileConfiguration != null && (!myManifestFileConfiguration.getClasspath().equals(getConfiguredClasspath())
           || !Comparing.equal(myManifestFileConfiguration.getMainClass(), getConfiguredMainClass())
           || !Comparing.equal(myManifestFileConfiguration.getManifestFilePath(), getConfiguredManifestPath()));
  }

  @Nullable
  private String getConfiguredManifestPath() {
    final String path = myManifestPathField.getText();
    return path.length() != 0 ? FileUtil.toSystemIndependentName(path) : null;
  }

  @Override
  public void apply() {
    if (myManifestFileConfiguration != null) {
      myManifestFileConfiguration.setMainClass(getConfiguredMainClass());
      myManifestFileConfiguration.setClasspath(getConfiguredClasspath());
      myManifestFileConfiguration.setManifestFilePath(getConfiguredManifestPath());
    }
  }

  private List<String> getConfiguredClasspath() {
    return StringUtil.split(myClasspathField.getText(), " ");
  }

  @Override
  @NotNull
  public JComponent createComponent() {
    return myMainPanel;
  }

  @Nullable
  private String getConfiguredMainClass() {
    final String className = myMainClassField.getText();
    return className.length() != 0 ? className : null;
  }

}
