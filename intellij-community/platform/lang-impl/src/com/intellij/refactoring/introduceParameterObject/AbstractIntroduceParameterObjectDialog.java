/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring.introduceParameterObject;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.AbstractParameterTablePanel;
import com.intellij.refactoring.util.AbstractVariableData;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public abstract class AbstractIntroduceParameterObjectDialog<M extends PsiNamedElement,
                                                             P extends ParameterInfo,
                                                             C extends IntroduceParameterObjectClassDescriptor<M, P>,
                                                             V extends AbstractVariableData> extends RefactoringDialog {
  protected M mySourceMethod;
  private JPanel myWholePanel;
  private JTextField mySourceMethodTextField;
  protected JCheckBox myDelegateCheckBox;
  private JPanel myParamsPanel;
  private JPanel myParameterClassPanel;

  protected AbstractParameterTablePanel<V> myParameterTablePanel;

  protected abstract String getSourceMethodPresentation();
  protected abstract JPanel createParameterClassPanel();
  protected abstract AbstractParameterTablePanel<V> createParametersPanel();

  protected abstract C createClassDescriptor();

  protected boolean isDelegateCheckboxVisible() {
    return true;
  }

  public AbstractIntroduceParameterObjectDialog(M method) {
    super(method.getProject(), true);
    mySourceMethod = method;
    setTitle(RefactoringBundle.message("refactoring.introduce.parameter.object.title"));
  }

  @Override
  protected void doAction() {
    final IntroduceParameterObjectDelegate<M, P, C> delegate = IntroduceParameterObjectDelegate.findDelegate(mySourceMethod);
    final List<P> allMethodParameters = delegate.getAllMethodParameters(mySourceMethod);
    invokeRefactoring(
      new IntroduceParameterObjectProcessor<>(mySourceMethod, createClassDescriptor(), allMethodParameters, keepMethodAsDelegate()));
  }

  @Override
  protected void canRun() throws ConfigurationException {
    if (!hasParametersToExtract()) {
      throw new ConfigurationException("Nothing found to extract");
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    mySourceMethodTextField.setText(getSourceMethodPresentation());
    mySourceMethodTextField.setEditable(false);

    myDelegateCheckBox.setVisible(isDelegateCheckboxVisible());

    myParameterTablePanel = createParametersPanel();
    myParamsPanel.add(myParameterTablePanel, BorderLayout.CENTER);

    myParameterClassPanel.add(createParameterClassPanel(), BorderLayout.CENTER);
    return myWholePanel;
  }

  protected boolean keepMethodAsDelegate() {
    return myDelegateCheckBox.isSelected();
  }

  public boolean hasParametersToExtract() {
    for (AbstractVariableData info : myParameterTablePanel.getVariableData()) {
      if (info.passAsParameter) {
        return true;
      }
    }
    return false;
  }
}
