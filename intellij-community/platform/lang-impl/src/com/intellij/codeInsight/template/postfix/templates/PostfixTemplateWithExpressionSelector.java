// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PostfixTemplateWithExpressionSelector extends PostfixTemplate {
  @NotNull
  private final PostfixTemplateExpressionSelector mySelector;

  /**
   * @deprecated use {@link #PostfixTemplateWithExpressionSelector(String, String, String, String, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
   */
  @Deprecated
  protected PostfixTemplateWithExpressionSelector(@NotNull String name,
                                                  @NotNull String key,
                                                  @NotNull String example,
                                                  @NotNull PostfixTemplateExpressionSelector selector) {
    this(null, name, key, example, selector, null);
  }

  /**
   * @deprecated use {@link #PostfixTemplateWithExpressionSelector(String, String, String, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
   */
  @Deprecated
  protected PostfixTemplateWithExpressionSelector(@NotNull String name,
                                                  @NotNull String example,
                                                  @NotNull PostfixTemplateExpressionSelector selector) {
    this(null, name, example, selector, null);
  }

  protected PostfixTemplateWithExpressionSelector(@Nullable String id,
                                                  @NotNull String name,
                                                  @NotNull String example,
                                                  @NotNull PostfixTemplateExpressionSelector selector,
                                                  @Nullable PostfixTemplateProvider provider) {
    super(id, name, example, provider);
    mySelector = selector;
  }

  protected PostfixTemplateWithExpressionSelector(@Nullable String id,
                                                  @NotNull String name,
                                                  @NotNull String key,
                                                  @NotNull String example,
                                                  @NotNull PostfixTemplateExpressionSelector selector,
                                                  @Nullable PostfixTemplateProvider provider) {
    super(id, name, key, example, provider);
    mySelector = selector;
  }

  @Override
  public final boolean isApplicable(@NotNull PsiElement context, @NotNull Document copyDocument, int newOffset) {
    return mySelector.hasExpression(context, copyDocument, newOffset);
  }

  @Override
  public final void expand(@NotNull PsiElement context, @NotNull final Editor editor) {
    List<PsiElement> expressions = mySelector.getExpressions(context,
                                                             editor.getDocument(),
                                                             editor.getCaretModel().getOffset());

    if (expressions.isEmpty()) {
      PostfixTemplatesUtils.showErrorHint(context.getProject(), editor);
      return;
    }

    if (expressions.size() == 1) {
      prepareAndExpandForChooseExpression(expressions.get(0), editor);
      return;
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      PsiElement item = ContainerUtil.getFirstItem(expressions);
      assert item != null;
      prepareAndExpandForChooseExpression(item, editor);
      return;
    }

    IntroduceTargetChooser.showChooser(
      editor, expressions,
      new Pass<PsiElement>() {
        @Override
        public void pass(@NotNull final PsiElement e) {
          prepareAndExpandForChooseExpression(e, editor);
        }
      },
      mySelector.getRenderer(),
      "Expressions", 0, ScopeHighlighter.NATURAL_RANGER
    );
  }

  protected void prepareAndExpandForChooseExpression(@NotNull PsiElement expression, @NotNull Editor editor) {
    ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance()
      .executeCommand(expression.getProject(), () -> expandForChooseExpression(expression, editor), "Expand postfix template",
                      PostfixLiveTemplate.POSTFIX_TEMPLATE_ID));
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  protected abstract void expandForChooseExpression(@NotNull PsiElement expression, @NotNull Editor editor);
}
