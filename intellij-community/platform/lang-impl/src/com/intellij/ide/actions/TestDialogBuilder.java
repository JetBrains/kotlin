// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.google.common.base.Preconditions;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.Map;

/**
 * To use inside unit-tests, provide an instance of {@link TestDialogBuilder.TestAnswers}
 * in {@link AnActionEvent#getDataContext()} with the key {@link TestDialogBuilder.TestAnswers#KEY}.
 */
@TestOnly
public class TestDialogBuilder implements CreateFileFromTemplateDialog.Builder {
  private final TestAnswers myAnswers;
  private InputValidator myValidator;

  TestDialogBuilder(@NotNull TestDialogBuilder.TestAnswers answers) {
    myAnswers = answers;
  }

  @Override
  public CreateFileFromTemplateDialog.Builder setTitle(@Nls String title) {
    return this;
  }

  @Override
  public CreateFileFromTemplateDialog.Builder setValidator(InputValidator validator) {
    myValidator = validator;
    return this;
  }

  @Override
  public CreateFileFromTemplateDialog.Builder addKind(@Nls @NotNull String kind, @Nullable Icon icon, @NotNull String templateName) {
    return this;
  }

  @Override
  public @Nullable Map<String, String> getCustomProperties() {
    return null;
  }

  @Override
  public <T extends PsiElement> @Nullable T show(@NotNull String errorTitle,
                                                 @Nullable String selectedItem,
                                                 @NotNull CreateFileFromTemplateDialog.FileCreator<T> creator) {
    if (myValidator != null) {
      Preconditions.checkState(myValidator.checkInput(myAnswers.myName), "The answer '%s' is not valid.", myAnswers.myName);
      Preconditions.checkState(myValidator.canClose(myAnswers.myName), "Can't close dialog with the answer '%s'.", myAnswers.myName);
    }
    if (myAnswers.myName != null && myAnswers.myTemplateName != null) {
      return creator.createFile(myAnswers.myName, myAnswers.myTemplateName);
    }
    return null;
  }

  @Override
  public <T extends PsiElement> void show(@NotNull String errorTitle,
                                          @Nullable String selectedItem,
                                          @NotNull CreateFileFromTemplateDialog.FileCreator<T> creator,
                                          Consumer<? super T> elementConsumer) {
    elementConsumer.consume(show(errorTitle, selectedItem, creator));
  }

  @TestOnly
  public static class TestAnswers {
    public static final DataKey<TestAnswers> KEY = DataKey.create("CreateFileFromTemplateDialog.TestDataContext");

    private final String myName;
    private final String myTemplateName;

    public TestAnswers(@Nullable String name, @Nullable String templateName) {
      myName = name;
      myTemplateName = templateName;
    }
  }
}
