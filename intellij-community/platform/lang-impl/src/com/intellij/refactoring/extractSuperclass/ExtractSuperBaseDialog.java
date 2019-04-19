// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.extractSuperclass;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.ui.DocCommentPanel;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.RecentsManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author dsl
 */
public abstract class ExtractSuperBaseDialog<ClassType extends PsiElement, MemberInfoType extends MemberInfoBase> extends RefactoringDialog {
  private final String myRefactoringName;
  protected final ClassType mySourceClass;
  protected PsiDirectory myTargetDirectory;
  protected final List<MemberInfoType> myMemberInfos;

  private JRadioButton myRbExtractSuperclass;
  private JRadioButton myRbExtractSubclass;

  private JTextField mySourceClassField;
  private JLabel myClassNameLabel;
  private JTextField myExtractedSuperNameField;
  protected JLabel myPackageNameLabel;
  protected ComponentWithBrowseButton myPackageNameField;
  protected DocCommentPanel myDocCommentPanel;
  private JPanel myDestinationRootPanel;

  protected abstract ComponentWithBrowseButton createPackageNameField();

  protected JPanel createDestinationRootPanel() {
    return null;
  }

  protected abstract JTextField createSourceClassField();

  protected abstract String getDocCommentPanelName();

  protected abstract String getExtractedSuperNameNotSpecifiedMessage();

  protected abstract BaseRefactoringProcessor createProcessor();

  protected abstract int getDocCommentPolicySetting();

  protected abstract void setDocCommentPolicySetting(int policy);

  @Nullable
  protected abstract String validateName(String name);
  
  @Nullable
  protected String validateQualifiedName(String packageName, @NotNull String extractedSuperName) {
    return null;
  }

  protected abstract String getTopLabelText();

  protected abstract String getClassNameLabelText();

  protected abstract String getPackageNameLabelText();

  @NotNull
  protected abstract String getEntityName();

  protected abstract void preparePackage() throws OperationFailedException;

  protected abstract String getDestinationPackageRecentKey();

  public ExtractSuperBaseDialog(Project project, ClassType sourceClass, List<MemberInfoType> members, String refactoringName) {
    super(project, true);
    myRefactoringName = refactoringName;

    mySourceClass = sourceClass;
    myMemberInfos = members;
    myTargetDirectory = mySourceClass.getContainingFile().getContainingDirectory();
  }

  @Override
  protected void init() {
    setTitle(myRefactoringName);

    myPackageNameField = createPackageNameField();
    myDestinationRootPanel = createDestinationRootPanel();
    mySourceClassField = createSourceClassField();
    myExtractedSuperNameField = createExtractedSuperNameField();

    myDocCommentPanel = new DocCommentPanel(getDocCommentPanelName());
    myDocCommentPanel.setPolicy(getDocCommentPolicySetting());

    super.init();
    updateDialog();
  }

  protected JTextField createExtractedSuperNameField() {
    return new JTextField();
  }

  protected JComponent createActionComponent() {
    Box box = Box.createHorizontalBox();
    final String s = StringUtil.decapitalize(getEntityName());
    myRbExtractSuperclass = new JRadioButton();
    myRbExtractSuperclass.setText(RefactoringBundle.message("extractSuper.extract", s));
    myRbExtractSubclass = new JRadioButton();
    myRbExtractSubclass.setText(RefactoringBundle.message("extractSuper.rename.original.class", s));
    myRbExtractSubclass.setEnabled(isPossibleToRenameOriginal());
    box.add(myRbExtractSuperclass);
    box.add(myRbExtractSubclass);
    box.add(Box.createHorizontalGlue());
    final ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbExtractSuperclass);
    buttonGroup.add(myRbExtractSubclass);
    customizeRadiobuttons(box, buttonGroup);
    myRbExtractSuperclass.setSelected(true);

    ItemListener listener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        updateDialog();
      }
    };
    myRbExtractSuperclass.addItemListener(listener);
    myRbExtractSubclass.addItemListener(listener);
    return box;
  }

  protected boolean isPossibleToRenameOriginal() {
    return true;
  }

  protected void customizeRadiobuttons(Box box, ButtonGroup buttonGroup) {
  }

  @Override
  protected JComponent createNorthPanel() {
      Box box = Box.createVerticalBox();

      JPanel _panel = new JPanel(new BorderLayout());
      _panel.add(new JLabel(getTopLabelText()), BorderLayout.NORTH);
      _panel.add(mySourceClassField, BorderLayout.CENTER);
      box.add(_panel);

      box.add(Box.createVerticalStrut(10));

      box.add(createActionComponent());

      box.add(Box.createVerticalStrut(10));

      myClassNameLabel = new JLabel();

      _panel = new JPanel(new BorderLayout());
      _panel.add(myClassNameLabel, BorderLayout.NORTH);
      _panel.add(myExtractedSuperNameField, BorderLayout.CENTER);
      box.add(_panel);
      box.add(Box.createVerticalStrut(5));

      _panel = new JPanel(new BorderLayout());
      myPackageNameLabel = new JLabel();

      _panel.add(myPackageNameLabel, BorderLayout.NORTH);
      _panel.add(myPackageNameField, BorderLayout.CENTER);
      if (myDestinationRootPanel != null) {
        _panel.add(myDestinationRootPanel, BorderLayout.SOUTH);
      }
      box.add(_panel);
      box.add(Box.createVerticalStrut(10));

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(box, BorderLayout.CENTER);
      return panel;
    }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myExtractedSuperNameField;
  }

  protected void updateDialog() {
    myClassNameLabel.setText(getClassNameLabelText());
    myPackageNameLabel.setText(getPackageNameLabelText());
    getPreviewAction().setEnabled(!isExtractSuperclass());
  }

  @NotNull
  public String getExtractedSuperName() {
    return myExtractedSuperNameField.getText().trim();
  }

  protected abstract String getTargetPackageName();

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }

  public int getDocCommentPolicy() {
    return myDocCommentPanel.getPolicy();
  }

  public boolean isExtractSuperclass() {
    return myRbExtractSuperclass != null && myRbExtractSuperclass.isSelected();
  }

  @Override
  protected void doAction() {
    final String[] errorString = new String[]{null};
    final String extractedSuperName = getExtractedSuperName();
    final String packageName = getTargetPackageName();
    RecentsManager.getInstance(myProject).registerRecentEntry(getDestinationPackageRecentKey(), packageName);

    if (extractedSuperName.isEmpty()) {
      // TODO just disable OK button
      errorString[0] = getExtractedSuperNameNotSpecifiedMessage();
      myExtractedSuperNameField.requestFocusInWindow();
    }
    else {
      String nameError = validateName(extractedSuperName);
      if (nameError == null) {
        nameError = validateQualifiedName(packageName, extractedSuperName);
      }
      if (nameError != null) {
        errorString[0] = nameError;
        myExtractedSuperNameField.requestFocusInWindow();
      }
      else {
        CommandProcessor.getInstance().executeCommand(myProject, () -> {
          try {
            preparePackage();
          }
          catch (IncorrectOperationException | OperationFailedException e) {
            errorString[0] = e.getMessage();
            myPackageNameField.requestFocusInWindow();
          }
        }, RefactoringBundle.message("create.directory"), null);
      }
    }
    if (errorString[0] != null) {
      if (errorString[0].length() > 0) {
        CommonRefactoringUtil.showErrorMessage(myRefactoringName, errorString[0], getHelpId(), myProject);
      }
      return;
    }

    if (!checkConflicts()) return;

    executeRefactoring();
    setDocCommentPolicySetting(getDocCommentPolicy());
    closeOKAction();
  }

  protected void executeRefactoring() {
    if (!isExtractSuperclass()) {
      invokeRefactoring(createProcessor());
    }
  }

  protected boolean checkConflicts() {
    return true;
  }

  protected static class OperationFailedException extends Exception {
    public OperationFailedException(String message) {
      super(message);
    }
  }

  public Collection<MemberInfoType> getSelectedMemberInfos() {
    ArrayList<MemberInfoType> result = new ArrayList<>(myMemberInfos.size());
    for (MemberInfoType info : myMemberInfos) {
      if (info.isChecked()) {
        result.add(info);
      }
    }
    return result;
  }
}