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
package com.intellij.refactoring;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.introduce.IntroduceTarget;
import com.intellij.refactoring.introduce.PsiIntroduceTarget;
import com.intellij.ui.JBColor;
import com.intellij.util.Function;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class IntroduceTargetChooser {
  private IntroduceTargetChooser() {
  }

  public static <T extends PsiElement> void showChooser(@NotNull Editor editor,
                                                        @NotNull List<? extends T> expressions,
                                                        @NotNull Pass<? super T> callback,
                                                        @NotNull Function<? super T, String> renderer) {
    showChooser(editor, expressions, callback, renderer, "Expressions");
  }

  public static <T extends PsiElement> void showChooser(@NotNull Editor editor,
                                                        @NotNull List<? extends T> expressions,
                                                        @NotNull Pass<? super T> callback,
                                                        @NotNull Function<? super T, String> renderer,
                                                        @NotNull @Nls String title) {
    showChooser(editor, expressions, callback, renderer, title, ScopeHighlighter.NATURAL_RANGER);
  }

  public static <T extends PsiElement> void showChooser(@NotNull Editor editor,
                                                        @NotNull List<? extends T> expressions,
                                                        @NotNull Pass<? super T> callback,
                                                        @NotNull Function<? super T, String> renderer,
                                                        @NotNull @Nls String title,
                                                        @NotNull NotNullFunction<? super PsiElement, ? extends TextRange> ranger) {
    showChooser(editor, expressions, callback, renderer, title, -1, ranger);
  }

  public static <T extends PsiElement> void showChooser(@NotNull Editor editor,
                                                        @NotNull List<? extends T> expressions,
                                                        @NotNull Pass<? super T> callback,
                                                        @NotNull Function<? super T, String> renderer,
                                                        @NotNull @Nls String title,
                                                        int selection,
                                                        @NotNull NotNullFunction<? super PsiElement, ? extends TextRange> ranger) {
    List<MyIntroduceTarget<T>> targets = ContainerUtil.map(expressions, t -> new MyIntroduceTarget<>(t, ranger, renderer));
    Pass<MyIntroduceTarget<T>> callbackWrapper = new Pass<MyIntroduceTarget<T>>() {
      @Override
      public void pass(MyIntroduceTarget<T> target) {
        callback.pass(target.getPlace());
      }
    };
    showIntroduceTargetChooser(editor, targets, callbackWrapper, title, selection);
  }

  public static <T extends IntroduceTarget> void showIntroduceTargetChooser(@NotNull Editor editor,
                                                                            @NotNull List<T> expressions,
                                                                            @NotNull Pass<? super T> callback,
                                                                            @NotNull @Nls String title,
                                                                            int selection) {
    AtomicReference<ScopeHighlighter> highlighter = new AtomicReference<>(new ScopeHighlighter(editor));

    JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(expressions)
      .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      .setSelectedValue(expressions.get(selection > -1 ? selection : 0), true)
      .setAccessibleName(title)
      .setTitle(title)
      .setMovable(false)
      .setResizable(false)
      .setRequestFocus(true)
      .setItemSelectedCallback((expr) -> {
        ScopeHighlighter h = highlighter.get();
        if (h == null) return;
        h.dropHighlight();
        if (expr != null && expr.isValid()) {
          TextRange range = expr.getTextRange();
          h.highlight(Pair.create(range, Collections.singletonList(range)));
        }
      })
      .setItemChosenCallback((expr) -> {
        if (expr.isValid()) {
          callback.pass(expr);
        }
      })
      .addListener(new JBPopupAdapter() {
        @Override
        public void onClosed(@NotNull LightweightWindowEvent event) {
          highlighter.getAndSet(null).dropHighlight();
        }
      })
      .setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
          Component rendererComponent =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          IntroduceTarget expr = (T)value;
          if (expr.isValid()) {
            String text = expr.render();
            int firstNewLinePos = text.indexOf('\n');
            String trimmedText =
              text.substring(0, firstNewLinePos != -1 ? firstNewLinePos : Math.min(100, text.length()));
            if (trimmedText.length() != text.length()) trimmedText += " ...";
            setText(trimmedText);
          }
          else {
            setForeground(JBColor.RED);
            setText("Invalid");
          }
          return rendererComponent;
        }
      }).createPopup();
    popup.showInBestPositionFor(editor);
    Project project = editor.getProject();
    if (project != null && !popup.isDisposed()) {
      NavigationUtil.hidePopupIfDumbModeStarts(popup, project);
    }
  }

  private static class MyIntroduceTarget<T extends PsiElement> extends PsiIntroduceTarget<T> {
    private final NotNullFunction<? super PsiElement, ? extends TextRange> myRanger;
    private final Function<? super T, String> myRenderer;

    MyIntroduceTarget(@NotNull T psi,
                             @NotNull NotNullFunction<? super PsiElement, ? extends TextRange> ranger,
                             @NotNull Function<? super T, String> renderer) {
      super(psi);
      myRanger = ranger;
      myRenderer = renderer;
    }

    @NotNull
    @Override
    public TextRange getTextRange() {
      return myRanger.fun(getPlace());
    }

    @NotNull
    @Override
    public String render() {
      return myRenderer.fun(getPlace());
    }

    @Override
    public String toString() {
      return isValid() ? render() : "invalid";
    }
  }
}
