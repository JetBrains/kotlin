// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates.editable;

import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

public abstract class EditablePostfixTemplateWithMultipleExpressions<ConditionType extends PostfixTemplateExpressionCondition>
  extends EditablePostfixTemplate {

  @NotNull protected final Set<ConditionType> myExpressionConditions;
  protected final boolean myUseTopmostExpression;

  protected EditablePostfixTemplateWithMultipleExpressions(@NotNull String templateId,
                                                           @NotNull String templateName,
                                                           @NotNull TemplateImpl liveTemplate,
                                                           @NotNull String example,
                                                           @NotNull Set<ConditionType> expressionConditions,
                                                           boolean useTopmostExpression,
                                                           @NotNull PostfixTemplateProvider provider) {
    super(templateId, templateName, liveTemplate, example, provider);
    myExpressionConditions = expressionConditions;
    myUseTopmostExpression = useTopmostExpression;
  }

  protected EditablePostfixTemplateWithMultipleExpressions(@NotNull String templateId,
                                                           @NotNull String templateName,
                                                           @NotNull String templateKey,
                                                           @NotNull TemplateImpl liveTemplate,
                                                           @NotNull String example,
                                                           @NotNull Set<ConditionType> expressionConditions,
                                                           boolean useTopmostExpression,
                                                           @NotNull PostfixTemplateProvider provider) {
    super(templateId, templateName, templateKey, liveTemplate, example, provider);
    myExpressionConditions = expressionConditions;
    myUseTopmostExpression = useTopmostExpression;
  }

  @NotNull
  protected static TemplateImpl createTemplate(@NotNull String templateText) {
    TemplateImpl template = new TemplateImpl("fakeKey", templateText, "");
    template.setToReformat(true);
    template.parseSegments();
    return template;
  }


  @NotNull
  @Override
  protected PsiElement getElementToRemove(@NotNull PsiElement element) {
    if (myUseTopmostExpression) {
      return getTopmostExpression(element);
    }
    return element;
  }

  @NotNull
  protected abstract PsiElement getTopmostExpression(@NotNull PsiElement element);

  @NotNull
  public Set<ConditionType> getExpressionConditions() {
    return myExpressionConditions;
  }

  public boolean isUseTopmostExpression() {
    return myUseTopmostExpression;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    EditablePostfixTemplateWithMultipleExpressions<?> that = (EditablePostfixTemplateWithMultipleExpressions<?>)o;
    return myUseTopmostExpression == that.myUseTopmostExpression &&
           Objects.equals(myExpressionConditions, that.myExpressionConditions);
  }

  @NotNull
  protected Condition<PsiElement> getExpressionCompositeCondition() {
    return e -> {
      for (ConditionType condition : myExpressionConditions) {
        //noinspection unchecked
        if (condition.value(e)) {
          return true;
        }
      }
      return myExpressionConditions.isEmpty();
    };
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), myExpressionConditions, myUseTopmostExpression);
  }
}
