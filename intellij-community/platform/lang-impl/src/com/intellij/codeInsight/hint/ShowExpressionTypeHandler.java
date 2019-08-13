// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.ExpressionTypeProvider;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExpressionTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.ui.LightweightHint;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ShowExpressionTypeHandler implements CodeInsightActionHandler {
  private final boolean myRequestFocus;

  public ShowExpressionTypeHandler(boolean requestFocus) {
    myRequestFocus = requestFocus;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
    final Set<ExpressionTypeProvider> handlers = getHandlers(project, language, file.getViewProvider().getBaseLanguage());
    if (handlers.isEmpty()) return;

    Map<PsiElement, ExpressionTypeProvider> map = getExpressions(file, editor, handlers);
    Pass<PsiElement> callback = new Pass<PsiElement>() {
      @Override
      public void pass(@NotNull PsiElement expression) {
        ExpressionTypeProvider provider = ObjectUtils.assertNotNull(map.get(expression));
        //noinspection unchecked
        final String informationHint = provider.getInformationHint(expression);
        TextRange range = expression.getTextRange();
        editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
        displayHint(new DisplayedTypeInfo(expression, provider, editor), informationHint);
      }
    };
    if (map.isEmpty()) {
      ApplicationManager.getApplication().invokeLater(() -> {
        String errorHint = ObjectUtils.assertNotNull(ContainerUtil.getFirstItem(handlers)).getErrorHint();
        HintManager.getInstance().showErrorHint(editor, errorHint);
      });
    }
    else if (map.size() == 1) {
      Map.Entry<PsiElement, ExpressionTypeProvider> entry = map.entrySet().iterator().next();
      PsiElement expression = entry.getKey();
      ExpressionTypeProvider provider = entry.getValue();
      DisplayedTypeInfo typeInfo = new DisplayedTypeInfo(expression, provider, editor);
      if (typeInfo.isRepeating() && provider.hasAdvancedInformation()) {
        //noinspection unchecked
        String informationHint = provider.getAdvancedInformationHint(expression);
        displayHint(typeInfo, informationHint);
      } else {
        callback.pass(expression);
      }
    }
    else {
      IntroduceTargetChooser.showChooser(
        editor, new ArrayList<>(map.keySet()), callback,
        PsiElement::getText
      );
    }
  }

  private void displayHint(@NotNull DisplayedTypeInfo typeInfo, String informationHint) {
    ApplicationManager.getApplication().invokeLater(() -> {
      HintManager.getInstance().setRequestFocusForNextHint(myRequestFocus);
      typeInfo.showHint(informationHint);
    });
  }

  @NotNull
  public Map<PsiElement, ExpressionTypeProvider> getExpressions(@NotNull PsiFile file,
                                                                @NotNull Editor editor) {
    Language language = PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset());
    Set<ExpressionTypeProvider> handlers = getHandlers(file.getProject(), language, file.getViewProvider().getBaseLanguage());
    return getExpressions(file, editor, handlers);
  }

  @NotNull
  private static Map<PsiElement, ExpressionTypeProvider> getExpressions(@NotNull PsiFile file,
                                                                        @NotNull Editor editor,
                                                                        @NotNull Set<? extends ExpressionTypeProvider> handlers) {
    if (handlers.isEmpty()) return Collections.emptyMap();
    boolean exactRange = false;
    TextRange range = EditorUtil.getSelectionInAnyMode(editor);
    final Map<PsiElement, ExpressionTypeProvider> map = new LinkedHashMap<>();
    int offset = !range.isEmpty() ? range.getStartOffset() : TargetElementUtil.adjustOffset(file, editor.getDocument(), range.getStartOffset());
    for (int i = 0; i < 3 && map.isEmpty() && offset >= i; i++) {
      PsiElement elementAt = file.findElementAt(offset - i);
      if (elementAt == null) continue;
      for (ExpressionTypeProvider handler : handlers) {
        for (PsiElement element : ((ExpressionTypeProvider<? extends PsiElement>)handler).getExpressionsAt(elementAt)) {
          TextRange r = element.getTextRange();
          if (exactRange && !r.equals(range) || !r.contains(range)) continue;
          if (!exactRange) exactRange = r.equals(range);
          map.put(element, handler);
        }
      }
    }
    return map;
  }

  @NotNull
  public static Set<ExpressionTypeProvider> getHandlers(final Project project, Language... languages) {
    DumbService dumbService = DumbService.getInstance(project);
    return JBIterable.of(languages).flatten(
      language -> dumbService.filterByDumbAwareness(LanguageExpressionTypes.INSTANCE.allForLanguage(language))).addAllTo(
      new LinkedHashSet<>());
  }

  static final class DisplayedTypeInfo {
    private static volatile DisplayedTypeInfo ourCurrentInstance;
    final @NotNull PsiElement myElement;
    final @NotNull ExpressionTypeProvider<?> myProvider;
    final @NotNull Editor myEditor;

    DisplayedTypeInfo(@NotNull PsiElement element, @NotNull ExpressionTypeProvider<?> provider, @NotNull Editor editor) {
      myElement = element;
      myProvider = provider;
      myEditor = editor;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DisplayedTypeInfo info = (DisplayedTypeInfo)o;
      return Objects.equals(myElement, info.myElement) &&
             Objects.equals(myProvider, info.myProvider) &&
             Objects.equals(myEditor, info.myEditor);
    }

    /**
     * @return true if the same hint (i.e. on the same PsiElement, with the same provider, in the same editor) is displayed currently.
     */
    boolean isRepeating() {
      return this.equals(ourCurrentInstance);
    }

    void showHint(String informationHint) {
      JComponent label = HintUtil.createInformationLabel(informationHint);
      setInstance(this);
      AccessibleContextUtil.setName(label, "Expression type hint");
      HintManagerImpl hintManager = (HintManagerImpl)HintManager.getInstance();
      LightweightHint hint = new LightweightHint(label);
      hint.addHintListener(e -> ApplicationManager.getApplication().invokeLater(() -> setInstance(null)));
      Point p = hintManager.getHintPosition(hint, myEditor, HintManager.ABOVE);
      int flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING;
      hintManager.showEditorHint(hint, myEditor, p, flags, 0, false);
    }

    private static void setInstance(DisplayedTypeInfo typeInfo) {
      ourCurrentInstance = typeInfo;
    }
  }
}

