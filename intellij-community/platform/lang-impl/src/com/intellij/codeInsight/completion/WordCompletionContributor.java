/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.LanguageWordCompletion;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.impl.cache.impl.id.IdTableBuilding;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author peter
 */
public class WordCompletionContributor extends CompletionContributor implements DumbAware {
  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    if (context.getCompletionType() == CompletionType.BASIC && isWordCompletionDefinitelyEnabled(context.getFile())) {
      context.setDummyIdentifier("");
    }
  }

  private static boolean isWordCompletionDefinitelyEnabled(@NotNull PsiFile file) {
    return DumbService.isDumb(file.getProject()) ||
           file instanceof PsiPlainTextFile && file.getViewProvider().getLanguages().size() == 1;
  }

  @Override
  public void fillCompletionVariants(@NotNull final CompletionParameters parameters, @NotNull final CompletionResultSet result) {
    if (parameters.getCompletionType() == CompletionType.BASIC && shouldPerformWordCompletion(parameters)) {
      addWordCompletionVariants(result, parameters, Collections.emptySet());
    }
  }

  public static void addWordCompletionVariants(CompletionResultSet result, final CompletionParameters parameters, Set<String> excludes) {
    addWordCompletionVariants(result, parameters, excludes, false);
  }

  public static void addWordCompletionVariants(CompletionResultSet result, final CompletionParameters parameters, Set<String> excludes, boolean allowEmptyPrefix) {
    final Set<String> realExcludes = new HashSet<>(excludes);
    for (String exclude : excludes) {
      String[] words = exclude.split("[ .-]");
      if (words.length > 0 && StringUtil.isNotEmpty(words[0])) {
        realExcludes.add(words[0]);
      }
    }

    int startOffset = parameters.getOffset();
    final PsiElement position = parameters.getPosition();
    final CompletionResultSet javaResultSet = result.withPrefixMatcher(CompletionUtil.findJavaIdentifierPrefix(parameters));
    final CompletionResultSet plainResultSet = result.withPrefixMatcher(CompletionUtil.findAlphanumericPrefix(parameters));
    consumeAllWords(position, startOffset, word -> {
      if (!realExcludes.contains(word)) {
        final LookupElement item = LookupElementBuilder.create(word);
        javaResultSet.addElement(item);
        plainResultSet.addElement(item);
      }
    }, allowEmptyPrefix);

    addValuesFromOtherStringLiterals(result, parameters, realExcludes, position);
  }

  private static void addValuesFromOtherStringLiterals(CompletionResultSet result,
                                                       CompletionParameters parameters,
                                                       final Set<String> realExcludes, PsiElement position) {
    ParserDefinition definition = LanguageParserDefinitions.INSTANCE.forLanguage(position.getLanguage());
    if (definition == null) {
      return;
    }
    final ElementPattern<PsiElement> pattern = psiElement().withElementType(definition.getStringLiteralElements());
    final PsiElement localString = PsiTreeUtil.findFirstParent(position, false, element -> pattern.accepts(element));
    if (localString == null) {
      return;
    }
    ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(localString);
    if (manipulator == null) {
      return;
    }
    int valueStart = manipulator.getRangeInElement(localString).getStartOffset() + localString.getTextRange().getStartOffset();
    if (valueStart > parameters.getOffset()) {
      return;
    }
    PsiFile file = position.getContainingFile();
    String prefix = file.getViewProvider().getContents().subSequence(valueStart, parameters.getOffset()).toString();
    CompletionResultSet fullStringResult = result.withPrefixMatcher(new PlainPrefixMatcher(prefix));
    file.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element == localString) {
          return;
        }
        if (pattern.accepts(element)) {
          element.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement each) {
              String valueText = ElementManipulators.getValueText(each);
              if (StringUtil.isNotEmpty(valueText) && !realExcludes.contains(valueText)) {
                final LookupElement item = LookupElementBuilder.create(valueText);
                fullStringResult.addElement(item);
              }
            }
          });
          return;
        }
        super.visitElement(element);
      }
    });
  }

  private static boolean shouldPerformWordCompletion(CompletionParameters parameters) {
    if (parameters.getInvocationCount() == 0) {
      return false;
    }

    PsiElement insertedElement = parameters.getPosition();
    PsiFile file = insertedElement.getContainingFile();
    if (isWordCompletionDefinitelyEnabled(file)) {
      return true;
    }

    final CompletionData data = CompletionUtil.getCompletionDataByElement(insertedElement, file);
    if (data != null) {
      Set<CompletionVariant> toAdd = new HashSet<>();
      data.addKeywordVariants(toAdd, insertedElement, file);
      for (CompletionVariant completionVariant : toAdd) {
        if (completionVariant.hasKeywordCompletions()) {
          return false;
        }
      }
    }

    final int startOffset = parameters.getOffset();

    final PsiReference reference = file.findReferenceAt(startOffset);
    if (reference != null) {
      return false;
    }

    final PsiElement element = file.findElementAt(startOffset - 1);

    ASTNode textContainer = element != null ? element.getNode() : null;
    while (textContainer != null) {
      if (LanguageWordCompletion.INSTANCE.isEnabledIn(textContainer.getElementType())) {
        return true;
      }
      textContainer = textContainer.getTreeParent();
    }
    return false;
  }

  private static void consumeAllWords(@NotNull PsiElement context,
                                      int offset,
                                      @NotNull Consumer<? super String> consumer,
                                      boolean allowEmptyPrefix) {
    if (!allowEmptyPrefix && StringUtil.isEmpty(CompletionUtil.findJavaIdentifierPrefix(context, offset))) return;
    CharSequence chars = context.getContainingFile().getViewProvider().getContents(); // ??
    Set<CharSequence> words = new THashSet<>(chars.length()/8);
    IdTableBuilding.scanWords((charSeq, charsArray, start, end) -> {
      if (start > offset || offset > end) {
        CharSequence sequence = charSeq.subSequence(start, end);
        String str = sequence.toString();
        if (words.add(str)) consumer.accept(str);
      }
    }, chars, 0, chars.length());
  }
}
