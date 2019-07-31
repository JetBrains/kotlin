// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.ide.actions.newclass.CreateWithTemplatesDialogPanel;
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.util.Consumer;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author peter
 */
public class CreateFileFromTemplateDialog extends DialogWrapper {
  private JTextField myNameField;
  private TemplateKindCombo myKindCombo;
  private JPanel myPanel;
  private JLabel myUpDownHint;
  private JLabel myKindLabel;
  private JLabel myNameLabel;

  private ElementCreator myCreator;
  private InputValidator myInputValidator;

  protected CreateFileFromTemplateDialog(@NotNull Project project) {
    super(project, true);

    myKindLabel.setLabelFor(myKindCombo);
    myKindCombo.registerUpDownHint(myNameField);
    myUpDownHint.setIcon(PlatformIcons.UP_DOWN_ARROWS);
    setTemplateKindComponentsVisible(false);
    init();
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if (myInputValidator != null) {
      final String text = myNameField.getText().trim();
      final boolean canClose = myInputValidator.canClose(text);
      if (!canClose) {
        String errorText = LangBundle.message("incorrect.name");
        if (myInputValidator instanceof InputValidatorEx) {
          String message = ((InputValidatorEx)myInputValidator).getErrorText(text);
          if (message != null) {
            errorText = message;
          }
        }
        return new ValidationInfo(errorText, myNameField);
      }
    }
    return super.doValidate();
  }

  protected JTextField getNameField() {
    return myNameField;
  }

  protected TemplateKindCombo getKindCombo() {
    return myKindCombo;
  }

  protected JLabel getKindLabel() {
    return myKindLabel;
  }

  protected JLabel getNameLabel() {
    return myNameLabel;
  }

  private String getEnteredName() {
    final JTextField nameField = getNameField();
    final String text = nameField.getText().trim();
    nameField.setText(text);
    return text;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  protected void doOKAction() {
    if (myCreator != null && myCreator.tryCreate(getEnteredName()).length == 0) {
      return;
    }
    super.doOKAction();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return getNameField();
  }

  public void setTemplateKindComponentsVisible(boolean flag) {
    myKindCombo.setVisible(flag);
    myKindLabel.setVisible(flag);
    myUpDownHint.setVisible(flag);
  }

  public static Builder createDialog(@NotNull final Project project) {
    if (Experiments.getInstance().isFeatureEnabled("show.create.new.element.in.popup")) {
     return new NonBlockingPopupBuilderImpl(project);
    }
    else {
      final CreateFileFromTemplateDialog dialog = new CreateFileFromTemplateDialog(project);
      return new BuilderImpl(dialog, project);
    }
  }

  private static class BuilderImpl implements Builder {
    private final CreateFileFromTemplateDialog myDialog;
    private final Project myProject;

    BuilderImpl(CreateFileFromTemplateDialog dialog, Project project) {
      myDialog = dialog;
      myProject = project;
    }

    @Override
    public Builder setTitle(String title) {
      myDialog.setTitle(title);
      return this;
    }

    @Override
    public Builder addKind(@NotNull String name, @Nullable Icon icon, @NotNull String templateName) {
      myDialog.getKindCombo().addItem(name, icon, templateName);
      if (myDialog.getKindCombo().getComboBox().getItemCount() > 1) {
        myDialog.setTemplateKindComponentsVisible(true);
      }
      return this;
    }

    @Override
    public Builder setValidator(InputValidator validator) {
      myDialog.myInputValidator = validator;
      return this;
    }

    @Override
    public <T extends PsiElement> T show(@NotNull String errorTitle, @Nullable String selectedTemplateName,
                                         @NotNull final FileCreator<T> creator) {
      final Ref<SmartPsiElementPointer<T>> created = Ref.create(null);
      myDialog.getKindCombo().setSelectedName(selectedTemplateName);
      myDialog.myCreator = new ElementCreator(myProject, errorTitle) {

        @Override
        protected PsiElement[] create(@NotNull String newName) {
          T element = creator.createFile(myDialog.getEnteredName(), myDialog.getKindCombo().getSelectedName());
          if (element != null) {
            created.set(SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element));
            return new PsiElement[]{element};
          }
          return PsiElement.EMPTY_ARRAY;
        }

        @Override
        public boolean startInWriteAction() {
          return creator.startInWriteAction();
        }

        @Override
        protected String getActionName(String newName) {
          return creator.getActionName(newName, myDialog.getKindCombo().getSelectedName());
        }
      };

      myDialog.show();
      if (myDialog.getExitCode() == OK_EXIT_CODE) {
        SmartPsiElementPointer<T> pointer = created.get();
        return pointer == null ? null : pointer.getElement();
      }
      return null;
    }

    @Override
    public <T extends PsiElement> void show(@NotNull String errorTitle,
                                            @Nullable String selectedItem,
                                            @NotNull FileCreator<T> creator,
                                            Consumer<? super T> elementConsumer) {
      T element = show(errorTitle, selectedItem, creator);
      elementConsumer.consume(element);
    }

    @Nullable
    @Override
    public Map<String,String> getCustomProperties() {
      return null;
    }
  }

  private static class NonBlockingPopupBuilderImpl implements Builder {
    @NotNull private final Project myProject;

    private String myTitle = "Title";
    private final List<Trinity<String, Icon, String>> myTemplatesList = new ArrayList<>();
    private InputValidator myInputValidator;

    private NonBlockingPopupBuilderImpl(@NotNull Project project) {myProject = project;}

    @Override
    public Builder setTitle(String title) {
      myTitle = title;
      return this;
    }

    @Override
    public Builder addKind(@NotNull String kind, @Nullable Icon icon, @NotNull String templateName) {
      myTemplatesList.add(Trinity.create(kind, icon, templateName));
      return this;
    }

    @Override
    public Builder setValidator(InputValidator validator) {
      myInputValidator = validator;
      return this;
    }

    @Nullable
    @Override
    public <T extends PsiElement> T show(@NotNull String errorTitle, @Nullable String selectedItem, @NotNull FileCreator<T> creator) {
      throw new UnsupportedOperationException("Modal dialog is not supported by this builder");
    }

    @Override
    public <T extends PsiElement> void show(@NotNull String errorTitle,
                                            @Nullable String selectedItem,
                                            @NotNull FileCreator<T> fileCreator,
                                            Consumer<? super T> elementConsumer) {
      CreateWithTemplatesDialogPanel contentPanel = new CreateWithTemplatesDialogPanel(myTemplatesList, selectedItem);
      ElementCreator elementCreator = new ElementCreator(myProject, errorTitle) {

        @Override
        protected PsiElement[] create(@NotNull String newName) {
          T element = fileCreator.createFile(contentPanel.getEnteredName(), contentPanel.getSelectedTemplate());
          return element != null ? new PsiElement[]{element} : PsiElement.EMPTY_ARRAY;
        }

        @Override
        public boolean startInWriteAction() {
          return fileCreator.startInWriteAction();
        }

        @Override
        protected String getActionName(String newName) {
          return fileCreator.getActionName(newName, contentPanel.getSelectedTemplate());
        }
      };

      JBPopup popup = NewItemPopupUtil.createNewItemPopup(myTitle, contentPanel, contentPanel.getNameField());
      contentPanel.setApplyAction(e -> {
        String newElementName = contentPanel.getEnteredName();
        if (StringUtil.isEmptyOrSpaces(newElementName)) return;

        boolean isValid = myInputValidator == null || myInputValidator.canClose(newElementName);
        if (isValid) {
          popup.closeOk(e);
          T createdElement = (T) createElement(newElementName, elementCreator);
          if (createdElement != null) {
            elementConsumer.consume(createdElement);
          }
        }
        else {
          String errorMessage = Optional.ofNullable(myInputValidator)
            .filter(validator -> validator instanceof InputValidatorEx)
            .map(validator -> ((InputValidatorEx)validator).getErrorText(newElementName))
            .orElse(LangBundle.message("incorrect.name"));
          contentPanel.setError(errorMessage);
        }
      });

      Disposer.register(popup, contentPanel);
      popup.showCenteredInCurrentWindow(myProject);
    }

    @Nullable
    @Override
    public Map<String, String> getCustomProperties() {
      return null;
    }

    @Nullable
    private static PsiElement createElement(String newElementName, ElementCreator creator) {
      PsiElement[] elements = creator.tryCreate(newElementName);
      return elements.length > 0 ? elements[0] : null;
    }
  }

  public interface Builder {
    Builder setTitle(String title);
    Builder setValidator(InputValidator validator);
    Builder addKind(@NotNull String kind, @Nullable Icon icon, @NotNull String templateName);
    @Nullable
    <T extends PsiElement> T show(@NotNull String errorTitle, @Nullable String selectedItem, @NotNull FileCreator<T> creator);

    <T extends PsiElement> void show(@NotNull String errorTitle, @Nullable String selectedItem, @NotNull FileCreator<T> creator, Consumer<? super T> elementConsumer);

    @Nullable
    Map<String,String> getCustomProperties();
  }

  public interface FileCreator<T> {

    @Nullable
    T createFile(@NotNull String name, @NotNull String templateName);

    @NotNull
    String getActionName(@NotNull String name, @NotNull String templateName);

    boolean startInWriteAction();
  }
}
