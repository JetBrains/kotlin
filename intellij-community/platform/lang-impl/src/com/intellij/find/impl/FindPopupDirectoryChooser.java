// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.find.FindBundle;
import com.intellij.find.FindInProjectSettings;
import com.intellij.find.FindModel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class FindPopupDirectoryChooser extends JPanel {
  @NotNull private final FindUIHelper myHelper;
  @NotNull private final Project myProject;
  @NotNull private final FindPopupPanel myFindPopupPanel;
  @NotNull private final ComboBox<String> myDirectoryComboBox;

  @SuppressWarnings("WeakerAccess")
  public FindPopupDirectoryChooser(@NotNull FindPopupPanel panel) {
    super(new BorderLayout());

    myHelper = panel.getHelper();
    myProject = panel.getProject();
    myFindPopupPanel = panel;
    myDirectoryComboBox = new ComboBox<>(200);
    myDirectoryComboBox.setEditable(true);

    Component editorComponent = myDirectoryComboBox.getEditor().getEditorComponent();
    if (editorComponent instanceof JTextField) {
      JTextField field = (JTextField)editorComponent;
      field.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          myFindPopupPanel.scheduleResultsUpdate();
        }
      });
      field.setColumns(40);
    }
    myDirectoryComboBox.setMaximumRowCount(8);

    ActionListener restartSearchListener = e -> myFindPopupPanel.scheduleResultsUpdate();
    myDirectoryComboBox.addActionListener(restartSearchListener);

    FixedSizeButton mySelectDirectoryButton = new FixedSizeButton(myDirectoryComboBox);
    TextFieldWithBrowseButton.MyDoClickAction.addTo(mySelectDirectoryButton, myDirectoryComboBox);
    mySelectDirectoryButton.setMargin(JBUI.emptyInsets());

    mySelectDirectoryButton.addActionListener(__ -> {
      FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      descriptor.setForcedToUseIdeaFileChooser(true);
      myFindPopupPanel.getCanClose().set(false);
      FileChooser.chooseFiles(descriptor, myProject, null,
                              VfsUtil.findFileByIoFile(new File(getDirectory()), true),
                              new FileChooser.FileChooserConsumer() {
        @Override
        public void consume(List<VirtualFile> files) {
          ApplicationManager.getApplication().invokeLater(() -> {
            myFindPopupPanel.getCanClose().set(true);
            IdeFocusManager.getInstance(myProject).requestFocus(myDirectoryComboBox.getEditor().getEditorComponent(), true);
            myHelper.getModel().setDirectoryName(files.get(0).getPresentableUrl());
            myDirectoryComboBox.getEditor().setItem(files.get(0).getPresentableUrl());
          });
        }

        @Override
        public void cancelled() {
          ApplicationManager.getApplication().invokeLater(() -> {
            myFindPopupPanel.getCanClose().set(true);
            IdeFocusManager.getInstance(myProject).requestFocus(myDirectoryComboBox.getEditor().getEditorComponent(), true);
          });
        }
      });
    });

    MyRecursiveDirectoryAction recursiveDirectoryAction = new MyRecursiveDirectoryAction();
    int mnemonicModifiers = SystemInfo.isMac ? InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK : InputEvent.ALT_DOWN_MASK;
    recursiveDirectoryAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mnemonicModifiers)), myFindPopupPanel);

    add(myDirectoryComboBox, BorderLayout.CENTER);
    JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));
    buttonsPanel.add(mySelectDirectoryButton);
    buttonsPanel.add(FindPopupPanel.createToolbar(recursiveDirectoryAction).getComponent()); //check if toolbar updates the button with no delays
    add(buttonsPanel, BorderLayout.EAST);
  }

  @SuppressWarnings("WeakerAccess")
  public void initByModel(@NotNull FindModel findModel) {
    final String directoryName = findModel.getDirectoryName();
    java.util.List<String> strings = FindInProjectSettings.getInstance(myProject).getRecentDirectories();

    if (myDirectoryComboBox.getItemCount() > 0) {
      myDirectoryComboBox.removeAllItems();
    }
    if (directoryName != null && !directoryName.isEmpty()) {
      strings.remove(directoryName);
      myDirectoryComboBox.addItem(directoryName);
    }
    for (int i = strings.size() - 1; i >= 0; i--) {
      myDirectoryComboBox.addItem(strings.get(i));
    }
    if (myDirectoryComboBox.getItemCount() == 0) {
      myDirectoryComboBox.addItem("");
    }
  }

  @NotNull
  public ComboBox getComboBox() {
    return myDirectoryComboBox;
  }

  @NotNull
  public String getDirectory() {
    return (String)myDirectoryComboBox.getEditor().getItem();
  }

  @Nullable
  public ValidationInfo validate(@NotNull FindModel model) {
    VirtualFile directory = FindInProjectUtil.getDirectory(model);
    if (directory == null) {
      return new ValidationInfo(FindBundle.message("find.directory.not.found.error", getDirectory()), myDirectoryComboBox);
    }
    return null;
  }

  private class MyRecursiveDirectoryAction extends DumbAwareToggleAction {
    MyRecursiveDirectoryAction() {
      super(FindBundle.message("find.recursively.hint"), null, AllIcons.Actions.ShowAsTree);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return myHelper.getModel().isWithSubdirectories();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      myHelper.getModel().setWithSubdirectories(state);
      myFindPopupPanel.scheduleResultsUpdate();
    }
  }
}
