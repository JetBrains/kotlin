// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class SurroundPostfixTemplateBase extends PostfixTemplateWithExpressionSelector {

  @NotNull protected final PostfixTemplatePsiInfo myPsiInfo;

  protected SurroundPostfixTemplateBase(@NotNull String name,
                                        @NotNull String descr,
                                        @NotNull PostfixTemplatePsiInfo psiInfo,
                                        @NotNull PostfixTemplateExpressionSelector selector) {
    super(name, descr, selector);
    myPsiInfo = psiInfo;
  }


  @Override
  public final void expandForChooseExpression(@NotNull PsiElement expression, @NotNull final Editor editor) {
    PsiElement replace = getReplacedExpression(expression);
    TextRange range = PostfixTemplatesUtils.surround(getSurrounder(), editor, replace);

    if (range != null) {
      editor.getCaretModel().moveToOffset(range.getStartOffset());
    }
  }

  protected PsiElement getReplacedExpression(@NotNull PsiElement expression) {
    PsiElement wrappedExpression = getWrappedExpression(expression);
    return expression.replace(wrappedExpression);
  }

  protected PsiElement getWrappedExpression(PsiElement expression) {
    if (StringUtil.isEmpty(getHead()) && StringUtil.isEmpty(getTail())) {
      return expression;
    }
    return createNew(expression);
  }

  protected PsiElement createNew(PsiElement expression) {
    return myPsiInfo.createExpression(expression, getHead(), getTail());
  }

  @NotNull
  protected String getHead() {
    return "";
  }

  @NotNull
  protected String getTail() {
    return "";
  }

  @NotNull
  protected abstract Surrounder getSurrounder();
}

