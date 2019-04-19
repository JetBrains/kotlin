/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.find.editorHeaderActions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.impl.cache.impl.id.IdTableBuilding;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.util.ArrayUtil;
import com.intellij.util.text.Matcher;
import com.intellij.util.ui.GraphicsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VariantsCompletionAction extends AnAction {
  private final JTextComponent myTextField;

  public VariantsCompletionAction(JTextComponent textField) {
    myTextField = textField;
    final AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION);
    if (action != null) {
      registerCustomShortcutSet(action.getShortcutSet(), myTextField);
    }
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
    if (editor == null) return;
    final String prefix = myTextField.getText().substring(0, myTextField.getCaretPosition());
    if (StringUtil.isEmpty(prefix)) return;

      final String[] array = calcWords(prefix, editor);
      if (array.length == 0) {
        return;
      }

      FeatureUsageTracker.getInstance().triggerFeatureUsed("find.completion");
      final JList list = new JBList(array) {
        @Override
        protected void paintComponent(final Graphics g) {
          GraphicsUtil.setupAntialiasing(g);
          super.paintComponent(g);
        }
      };
      list.setBackground(new JBColor(new Color(235, 244, 254), new Color(0x4C4F51)));
      list.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));

      Utils.showCompletionPopup(
        e.getInputEvent() instanceof MouseEvent ? myTextField: null,
        list, null, myTextField, null);
  }

  private static String[] calcWords(final String prefix, Editor editor) {
    final Matcher matcher = NameUtil.buildMatcher(prefix, 0, true, true);
    final Set<String> words = new HashSet<>();
    CharSequence chars = editor.getDocument().getCharsSequence();

    IdTableBuilding.scanWords(new IdTableBuilding.ScanWordProcessor() {
        @Override
        public void run(final CharSequence chars, @Nullable char[] charsArray, final int start, final int end) {
          final String word = chars.subSequence(start, end).toString();
          if (matcher.matches(word)) {
            words.add(word);
          }
        }
      }, chars, 0, chars.length());


    ArrayList<String> sortedWords = new ArrayList<>(words);
    Collections.sort(sortedWords);

    return ArrayUtil.toStringArray(sortedWords);
  }
}
