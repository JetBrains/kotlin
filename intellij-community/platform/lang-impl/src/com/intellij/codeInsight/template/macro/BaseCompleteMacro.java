// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.completion.CompletionPhase;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.lookup.*;
import com.intellij.codeInsight.template.*;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public abstract class BaseCompleteMacro extends Macro {
  private final String myName;
  private final boolean myCheckCompletionChar;

  protected BaseCompleteMacro(@NonNls final String name) {
    this(name, true);
  }

  protected BaseCompleteMacro(@NonNls final String name, final boolean checkCompletionChar) {
    myName = name;
    myCheckCompletionChar = checkCompletionChar;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getPresentableName() {
    return myName + "()";
  }

  @Override
  @NotNull
  public String getDefaultValue() {
    return "a";
  }

  @Override
  public final Result calculateResult(@NotNull Expression[] params, final ExpressionContext context) {
    return new InvokeActionResult(
      () -> invokeCompletion(context)
    );
  }

  private void invokeCompletion(final ExpressionContext context) {
    final Project project = context.getProject();
    final Editor editor = context.getEditor();

    final PsiFile psiFile = editor != null ? PsiUtilBase.getPsiFileInEditor(editor, project) : null;
    Runnable runnable = () -> {
      if (project.isDisposed() || editor == null || editor.isDisposed() || psiFile == null || !psiFile.isValid()) return;

      // it's invokeLater, so another completion could have started
      if (CompletionServiceImpl.getCompletionService().getCurrentCompletion() != null) return;

      CommandProcessor.getInstance().executeCommand(project, () -> {
        // if we're in some completion's insert handler, make sure our new completion isn't treated as the second invocation
        CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion);

        invokeCompletionHandler(project, editor);
        Lookup lookup = LookupManager.getInstance(project).getActiveLookup();

        if (lookup != null) {
          lookup.addLookupListener(new MyLookupListener(context, myCheckCompletionChar));
        }
      }, "", null);
    };
    ApplicationManager.getApplication().invokeLater(runnable);
  }

  protected abstract void invokeCompletionHandler(Project project, Editor editor);

  @TestOnly
  public static void waitForNextTab() {
    UIUtil.dispatchAllInvocationEvents();
  }

  private static class MyLookupListener implements LookupListener {
    private final ExpressionContext myContext;
    private final boolean myCheckCompletionChar;

    MyLookupListener(@NotNull final ExpressionContext context, final boolean checkCompletionChar) {
      myContext = context;
      myCheckCompletionChar = checkCompletionChar;
    }

    @Override
    public void itemSelected(@NotNull LookupEvent event) {
      LookupElement item = event.getItem();
      if (item == null) return;

      char c = event.getCompletionChar();
      if (myCheckCompletionChar && !LookupEvent.isSpecialCompletionChar(c)) {
        return;
      }

      final Project project = myContext.getProject();
      if (project == null) {
        return;
      }

      ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, ()-> {
          Editor editor = myContext.getEditor();
          if (editor != null) {
            TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
            if (templateState != null) {
              templateState.considerNextTabOnLookupItemSelected(item);
            }
          }
        }), ModalityState.current(), project.getDisposed());

    }
  }
}
