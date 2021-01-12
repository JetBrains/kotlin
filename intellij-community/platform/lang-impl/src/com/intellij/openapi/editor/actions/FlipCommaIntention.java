/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.*;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlipCommaIntention implements IntentionAction {
  @NotNull
  @Override
  public String getText() {
    return getFamilyName();
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return "Flip ','";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    PsiElement comma = currentCommaElement(editor, file);
    if (comma == null) {
      return false;
    }
    final PsiElement left = smartAdvance(comma, false);
    final PsiElement right = smartAdvance(comma, true);
    return left != null && right != null && !left.getText().equals(right.getText()) && Flipper.isCanFlip(left, right);
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
    final PsiElement element = currentCommaElement(editor, file);
    if (element != null) {
      swapAtComma(element);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  private static void swapAtComma(@NotNull PsiElement comma) {
    PsiElement prev = smartAdvance(comma, false);
    PsiElement next = smartAdvance(comma, true);
    if (prev != null && next != null) {
      if (Flipper.tryFlip(prev, next)) {
        return;
      }
      swapViaDocument(comma, prev, next);
    }
  }

  // not via PSI because such language-unaware change can lead to PSI-text inconsistencies
  private static void swapViaDocument(@NotNull PsiElement comma, PsiElement prev, PsiElement next) {
    DocumentEx document = (DocumentEx)comma.getContainingFile().getViewProvider().getDocument();
    if (document == null) return;

    String prevText = prev.getText();
    String nextText = next.getText();

    TextRange prevRange = prev.getTextRange();
    TextRange nextRange = next.getTextRange();

    document.replaceString(prevRange.getStartOffset(), prevRange.getEndOffset(), nextText);
    nextRange = nextRange.shiftRight(nextText.length() - prevText.length());
    document.replaceString(nextRange.getStartOffset(), nextRange.getEndOffset(), prevText);
  }

  public interface Flipper {
    LanguageExtension<Flipper> EXTENSION = new LanguageExtension<>("com.intellij.flipCommaIntention.flipper");

    /**
     * @return true, if the elements were flipped; false, if the default flip implementation should be used.
     */
    boolean flip(@NotNull PsiElement left, @NotNull PsiElement right);

    /**
     * @return false, if the elements should not be flipped; true, if the default flip implementation should be used.
     */
    default boolean canFlip(@NotNull PsiElement left, @NotNull PsiElement right) {
      return true;
    }

    static boolean tryFlip(PsiElement left, PsiElement right) {
      final Language language = left.getLanguage();
      for (Flipper handler : EXTENSION.allForLanguage(language)) {
        if (handler.flip(left, right)) {
          return true;
        }
      }
      return false;
    }

    static boolean isCanFlip(PsiElement left, PsiElement right) {
      final Language language = left.getLanguage();
      for (Flipper handler : EXTENSION.allForLanguage(language)) {
        if (!handler.canFlip(left, right)) {
          return false;
        }
      }
      return true;
    }
  }

  private static PsiElement currentCommaElement(@NotNull Editor editor, @NotNull PsiFile file) {
    PsiElement element;
    if (!isComma(element = leftElement(editor, file)) && !isComma(element = rightElement(editor, file))) {
      return null;
    }
    return element;
  }

  @Nullable
  private static PsiElement leftElement(@NotNull Editor editor, @NotNull PsiFile file) {
    return file.findElementAt(editor.getCaretModel().getOffset() - 1);
  }

  @Nullable
  private static PsiElement rightElement(@NotNull Editor editor, @NotNull PsiFile file) {
    return file.findElementAt(editor.getCaretModel().getOffset());
  }

  private static boolean isComma(@Nullable PsiElement element) {
    return element != null && element.textMatches(",");
  }

  @NotNull
  private static JBIterable<PsiElement> getSiblings(PsiElement element, boolean fwd) {
    SyntaxTraverser.ApiEx<PsiElement> api = fwd ? SyntaxTraverser.psiApi() : SyntaxTraverser.psiApiReversed();
    JBIterable<PsiElement> flatSiblings = JBIterable.generate(element, api::next).skip(1);
    return SyntaxTraverser.syntaxTraverser(api)
      .withRoots(flatSiblings)
      .expandAndSkip(e -> api.typeOf(e) == GeneratedParserUtilBase.DUMMY_BLOCK)
      .traverse();
  }

  private static boolean isFlippable(PsiElement e) {
    if (e instanceof PsiWhiteSpace || e instanceof PsiComment) return false;
    return StringUtil.isNotEmpty(e.getText());
  }

  @Nullable
  private static PsiElement smartAdvance(PsiElement element, boolean fwd) {
    final PsiElement candidate = getSiblings(element, fwd).filter(e -> isFlippable(e)).first();
    if (candidate != null && isBrace(candidate)) return null;
    return candidate;
  }

  private static boolean isBrace(@NotNull PsiElement candidate) {
    final ASTNode node = candidate.getNode();
    if (node != null && node.getFirstChildNode() == null) {
      final PairedBraceMatcher braceMatcher = LanguageBraceMatching.INSTANCE.forLanguage(candidate.getLanguage());
      if (braceMatcher != null) {
        final IElementType elementType = node.getElementType();
        for (BracePair pair : braceMatcher.getPairs()) {
          if (elementType == pair.getLeftBraceType() || elementType == pair.getRightBraceType()) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
