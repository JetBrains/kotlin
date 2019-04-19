/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.arrangement.ArrangementUtil;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.CompositeArrangementSettingsToken;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.General.TYPE;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Regexp.TEXT;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Section.END_SECTION;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Section.START_SECTION;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementSectionRuleManager {
  private static final Set<ArrangementSettingsToken> MUTEXES = ContainerUtil.newHashSet(START_SECTION, END_SECTION);
  private static final Set<ArrangementSettingsToken> TOKENS = ContainerUtilRt.newHashSet(START_SECTION, END_SECTION, TEXT);

  private final Commenter myCommenter;

  private final ArrangementSectionRulesControl myControl;
  private final ArrangementMatchingRuleEditor myEditor;

  @Nullable
  public static ArrangementSectionRuleManager getInstance(@NotNull Language language,
                                                          @NotNull ArrangementStandardSettingsManager settingsManager,
                                                          @NotNull ArrangementColorsProvider colorsProvider,
                                                          @NotNull ArrangementSectionRulesControl control) {
    if (settingsManager.isSectionRulesSupported()) {
      return new ArrangementSectionRuleManager(language, settingsManager, colorsProvider, control);
    }
    return null;
  }

  private ArrangementSectionRuleManager(@NotNull Language language,
                                        @NotNull ArrangementStandardSettingsManager settingsManager,
                                        @NotNull ArrangementColorsProvider colorsProvider,
                                        @NotNull ArrangementSectionRulesControl control) {
    myCommenter = LanguageCommenters.INSTANCE.forLanguage(language);
    myControl = control;
    final List<CompositeArrangementSettingsToken> tokens = ContainerUtil.newArrayList();
    tokens.add(new CompositeArrangementSettingsToken(TYPE, ContainerUtil.newArrayList(START_SECTION, END_SECTION)));
    tokens.add(new CompositeArrangementSettingsToken(TEXT));
    myEditor = new ArrangementMatchingRuleEditor(settingsManager, tokens, colorsProvider, control);
  }

  public ArrangementMatchingRuleEditor getEditor() {
    return myEditor;
  }

  @NotNull
  public static Set<ArrangementSettingsToken> getSectionMutexes() {
    return MUTEXES;
  }

  public static boolean isEnabled(@NotNull ArrangementSettingsToken token) {
    return TOKENS.contains(token);
  }

  public void showEditor(int rowToEdit) {
    myControl.showEditor(myEditor, rowToEdit);
  }

  public boolean isSectionRule(@Nullable Object element) {
    return element instanceof StdArrangementMatchRule && getSectionRuleData((StdArrangementMatchRule)element) != null;
  }

  @Nullable
  public ArrangementSectionRuleData getSectionRuleData(@NotNull StdArrangementMatchRule element) {
    final ArrangementMatchCondition condition = element.getMatcher().getCondition();
    return getSectionRuleData(condition);
  }

  @Nullable
  public ArrangementSectionRuleData getSectionRuleData(@NotNull ArrangementMatchCondition condition) {
    final Ref<Boolean> isStart = new Ref<>();
    final Ref<String> text = new Ref<>();
    condition.invite(new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@NotNull ArrangementAtomMatchCondition condition) {
        final ArrangementSettingsToken type = condition.getType();
        if (type.equals(START_SECTION)) {
          isStart.set(true);
        }
        else if (type.equals(END_SECTION)) {
          isStart.set(false);
        }
        else if (type.equals(TEXT)) {
          text.set(condition.getValue().toString());
        }
      }

      @Override
      public void visit(@NotNull ArrangementCompositeMatchCondition condition) {
        for (ArrangementMatchCondition c : condition.getOperands()) {
          c.invite(this);
          if (!text.isNull() && !isStart.isNull()) {
            return;
          }
        }
      }
    });

    if (isStart.isNull()) {
      return null;
    }
    return new ArrangementSectionRuleData(processSectionText(StringUtil.notNullize(text.get())), isStart.get());
  }

  @NotNull
  public StdArrangementMatchRule createDefaultSectionRule() {
    final ArrangementAtomMatchCondition type = new ArrangementAtomMatchCondition(START_SECTION);
    final ArrangementAtomMatchCondition text = new ArrangementAtomMatchCondition(TEXT, createDefaultSectionText());
    final ArrangementMatchCondition condition = ArrangementUtil.combine(type, text);
    return new StdArrangementMatchRule(new StdArrangementEntryMatcher(condition));
  }

  @NotNull
  private String processSectionText(@NotNull String text) {
    final String lineCommentPrefix = myCommenter.getLineCommentPrefix();
    if (lineCommentPrefix != null && text.startsWith(lineCommentPrefix)) {
      return text;
    }

    final String prefix = myCommenter.getBlockCommentPrefix();
    final String suffix = myCommenter.getBlockCommentSuffix();
    if (prefix != null && suffix != null &&
        text.length() >= prefix.length() + suffix.length() && text.startsWith(prefix) && text.endsWith(suffix)) {
      return text;
    }
    return lineCommentPrefix != null ? wrapIntoLineComment(lineCommentPrefix, text) :
           prefix != null && suffix != null ? wrapIntoBlockComment(prefix, suffix, text) : "";
  }

  @NotNull
  private String createDefaultSectionText() {
    if (myCommenter != null) {
      final String lineCommentPrefix = myCommenter.getLineCommentPrefix();
      if (StringUtil.isNotEmpty(lineCommentPrefix)) {
        return wrapIntoLineComment(lineCommentPrefix, "");
      }

      final String prefix = myCommenter.getBlockCommentPrefix();
      final String suffix = myCommenter.getBlockCommentSuffix();
      if (StringUtil.isNotEmpty(prefix) && StringUtil.isNotEmpty(suffix)) {
        return wrapIntoBlockComment(prefix, suffix, " ");
      }
    }
    return "";
  }

  private static String wrapIntoBlockComment(@NotNull String prefix, @NotNull String suffix, @NotNull String text) {
    return prefix + text + suffix;
  }

  private static String wrapIntoLineComment(@NotNull String lineCommentPrefix, @NotNull String text) {
    return lineCommentPrefix + text;
  }

  public static class ArrangementSectionRuleData {
    private final boolean myIsSectionStart;
    private final String myText;

    private ArrangementSectionRuleData(@NotNull String text, boolean isStart) {
      myText = text;
      myIsSectionStart = isStart;
    }

    public boolean isSectionStart() {
      return myIsSectionStart;
    }

    @NotNull
    public String getText() {
      return myText;
    }
  }
}
