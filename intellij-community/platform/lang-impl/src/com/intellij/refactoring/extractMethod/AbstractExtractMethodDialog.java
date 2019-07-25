// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.extractMethod;

import com.intellij.codeInsight.codeFragment.CodeFragment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.ui.ComboBoxVisibilityPanel;
import com.intellij.refactoring.ui.MethodSignatureComponent;
import com.intellij.refactoring.util.AbstractVariableData;
import com.intellij.refactoring.util.SimpleParameterTablePanel;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.*;

public class AbstractExtractMethodDialog<T> extends DialogWrapper implements ExtractMethodSettings<T> {
  private JPanel myContentPane;
  private SimpleParameterTablePanel myParametersPanel;
  private JTextField myMethodNameTextField;
  private MethodSignatureComponent mySignaturePreviewTextArea;
  private JTextArea myOutputVariablesTextArea;
  private final ComboBoxVisibilityPanel<T> myVisibilityComboBox;
  private final Project myProject;
  private final String myDefaultName;
  private final ExtractMethodValidator myValidator;
  private final ExtractMethodDecorator<T> myDecorator;

  private AbstractVariableData[] myVariableData;
  private Map<String, AbstractVariableData> myVariablesMap;

  private final List<String> myArguments;
  private final ArrayList<String> myOutputVariables;
  private final FileType myFileType;

  public AbstractExtractMethodDialog(final Project project,
                                     final String defaultName,
                                     final CodeFragment fragment,
                                     final T[] visibilityVariants,
                                     final ExtractMethodValidator validator,
                                     final ExtractMethodDecorator<T> decorator,
                                     final FileType type) {
    super(project, true);
    myProject = project;
    myDefaultName = defaultName;
    myValidator = validator;
    myDecorator = decorator;
    myFileType = type;

    myVisibilityComboBox = new ComboBoxVisibilityPanel<>(visibilityVariants);
    myVisibilityComboBox.setVisible(visibilityVariants.length > 1);

    $$$setupUI$$$();

    myArguments = new ArrayList<>(fragment.getInputVariables());
    Collections.sort(myArguments);
    myOutputVariables = new ArrayList<>(fragment.getOutputVariables());
    Collections.sort(myOutputVariables);
    setModal(true);
    setTitle(RefactoringBundle.message("extract.method.title"));
    init();
  }

  private void $$$setupUI$$$() {
  }

  @Override
  protected void init() {
    super.init();
    // Set default name and select it
    myMethodNameTextField.setText(myDefaultName);
    myMethodNameTextField.setSelectionStart(0);
    myMethodNameTextField.setSelectionStart(myDefaultName.length());
    myMethodNameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        updateOutputVariables();
        updateSignature();
        updateOkStatus();
      }
    });


    myVariableData = createVariableDataByNames(myArguments);
    myVariablesMap = createVariableMap(myVariableData);
    myParametersPanel.init(myVariableData);

    updateOutputVariables();
    updateSignature();
    updateOkStatus();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMethodNameTextField;
  }

  public static AbstractVariableData[] createVariableDataByNames(final List<String> args) {
    final AbstractVariableData[] datas = new AbstractVariableData[args.size()];
    for (int i = 0; i < args.size(); i++) {
      final AbstractVariableData data = new AbstractVariableData();
      final String name = args.get(i);
      data.originalName = name;
      data.name = name;
      data.passAsParameter = true;
      datas[i] = data;
    }
    return datas;
  }

  public static Map<String, AbstractVariableData> createVariableMap(final AbstractVariableData[] data) {
    final HashMap<String, AbstractVariableData> map = new HashMap<>();
    for (AbstractVariableData variableData : data) {
      map.put(variableData.getOriginalName(), variableData);
    }
    return map;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  protected void doOKAction() {
    final String error = myValidator.check(getMethodName());
    if (error != null){
      if (ApplicationManager.getApplication().isUnitTestMode()){
        Messages.showInfoMessage(error, RefactoringBundle.message("error.title"));
        return;
      }
      if (Messages.showOkCancelDialog(error + ". " + RefactoringBundle.message("do.you.wish.to.continue"), RefactoringBundle.message("warning.title"), Messages.getWarningIcon()) != Messages.OK){
        return;
      }
    }
    super.doOKAction();
  }

  @Override
  protected String getHelpId() {
    return "refactoring.extractMethod";
  }

  @Override
  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  private void createUIComponents() {
    myParametersPanel = new SimpleParameterTablePanel(myValidator::isValidName) {
      @Override
      protected void doCancelAction() {
        AbstractExtractMethodDialog.this.doCancelAction();
      }

      @Override
      protected void doEnterAction() {
        doOKAction();
      }

      @Override
      protected void updateSignature() {
        updateOutputVariables();
        AbstractExtractMethodDialog.this.updateSignature();
      }
    };
    mySignaturePreviewTextArea = new MethodSignatureComponent("", myProject, myFileType);
  }

  private void updateOutputVariables() {
    final StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String variable : myOutputVariables) {
      if (myVariablesMap!=null){
        final AbstractVariableData data = myVariablesMap.get(variable);
        final String outputName = data != null ? data.getName() : variable;
        if (first){
          first = false;
        } else {
          builder.append(", ");
        }
        builder.append(outputName);
      }
    }
    myOutputVariablesTextArea.setText(builder.length() > 0 ? builder.toString() : RefactoringBundle.message("refactoring.extract.method.dialog.empty"));
  }

  private void updateSignature() {
    mySignaturePreviewTextArea.setSignature(myDecorator.createMethodSignature(this));
  }

  private void updateOkStatus() {
    setOKActionEnabled(myValidator.isValidName(getMethodName()));
  }

  @NotNull
  @Override
  public String getMethodName() {
    return myMethodNameTextField.getText().trim();
  }

  @NotNull
  @Override
  public AbstractVariableData[] getAbstractVariableData() {
    return myVariableData;
  }

  @Nullable
  @Override
  public T getVisibility() {
    return myVisibilityComboBox.getVisibility();
  }
}