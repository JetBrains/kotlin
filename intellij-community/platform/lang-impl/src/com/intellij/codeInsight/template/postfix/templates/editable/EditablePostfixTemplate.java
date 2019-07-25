// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates.editable;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatesUtils;
import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@ApiStatus.Experimental
public abstract class EditablePostfixTemplate extends PostfixTemplate {
  @NotNull private final TemplateImpl myLiveTemplate;

  public EditablePostfixTemplate(@NotNull String templateId,
                                 @NotNull String templateName,
                                 @NotNull TemplateImpl liveTemplate,
                                 @NotNull String example,
                                 @NotNull PostfixTemplateProvider provider) {
    this(templateId, templateName, "." + templateName, liveTemplate, example, provider);
  }

  public EditablePostfixTemplate(@NotNull String templateId,
                                 @NotNull String templateName,
                                 @NotNull String templateKey,
                                 @NotNull TemplateImpl liveTemplate,
                                 @NotNull String example,
                                 @NotNull PostfixTemplateProvider provider) {
    super(templateId, templateName, templateKey, example, provider);
    assert StringUtil.isNotEmpty(liveTemplate.getKey());
    myLiveTemplate = liveTemplate;
  }

  @NotNull
  public TemplateImpl getLiveTemplate() {
    return myLiveTemplate;
  }

  @Override
  public final void expand(@NotNull PsiElement context, @NotNull final Editor editor) {
    List<PsiElement> expressions = getExpressions(context, editor.getDocument(), editor.getCaretModel().getOffset());

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
      getElementRenderer(),
      "Expressions", 0, ScopeHighlighter.NATURAL_RANGER
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EditablePostfixTemplate)) return false;
    if (!super.equals(o)) return false;
    EditablePostfixTemplate template = (EditablePostfixTemplate)o;
    return Objects.equals(myLiveTemplate, template.myLiveTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getKey(), myLiveTemplate);
  }

  protected abstract List<PsiElement> getExpressions(@NotNull PsiElement context, @NotNull Document document, int offset);

  @Override
  public boolean isApplicable(@NotNull PsiElement context, @NotNull Document copyDocument, int newOffset) {
    return !getExpressions(context, copyDocument, newOffset).isEmpty();
  }

  protected void addTemplateVariables(@NotNull PsiElement element, @NotNull Template template) {
  }

  @NotNull
  protected PsiElement getElementToRemove(@NotNull PsiElement element) {
    return element;
  }

  @NotNull
  protected Function<PsiElement, String> getElementRenderer() {
    return element -> element.getText();
  }

  @NotNull
  @Override
  public PostfixTemplateProvider getProvider() {
    PostfixTemplateProvider provider = super.getProvider();
    assert provider != null;
    return provider;
  }

  private void prepareAndExpandForChooseExpression(@NotNull PsiElement element, @NotNull Editor editor) {
    ApplicationManager.getApplication().runWriteAction(
      () -> CommandProcessor.getInstance().executeCommand(
        element.getProject(), () -> expandForChooseExpression(element, editor), "Expand postfix template",
        PostfixLiveTemplate.POSTFIX_TEMPLATE_ID));
  }

  private void expandForChooseExpression(@NotNull PsiElement element, @NotNull Editor editor) {
    Project project = element.getProject();
    Document document = editor.getDocument();
    PsiElement elementToRemove = getElementToRemove(element);
    document.deleteString(elementToRemove.getTextRange().getStartOffset(), elementToRemove.getTextRange().getEndOffset());
    TemplateManager manager = TemplateManager.getInstance(project);

    TemplateImpl template = myLiveTemplate.copy();
    template.addVariable("EXPR", new TextExpression(element.getText()), false);
    addTemplateVariables(element, template);
    manager.startTemplate(editor, template);
  }
}
