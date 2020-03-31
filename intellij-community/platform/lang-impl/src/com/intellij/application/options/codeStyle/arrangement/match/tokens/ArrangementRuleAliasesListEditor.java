// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.match.tokens;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NamedItemsListEditor;
import com.intellij.openapi.ui.Namer;
import com.intellij.openapi.util.Cloner;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import gnu.trove.Equality;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jdom.Verifier;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasesListEditor extends NamedItemsListEditor<StdArrangementRuleAliasToken> {
  private static final Namer<StdArrangementRuleAliasToken> NAMER = new Namer<StdArrangementRuleAliasToken>() {
    @Override
    public String getName(StdArrangementRuleAliasToken token) {
      return token.getName();
    }

    @Override
    public boolean canRename(StdArrangementRuleAliasToken item) {
      return false;
    }

    @Override
    public void setName(StdArrangementRuleAliasToken token, String name) {
      token.setTokenName(name.replaceAll("\\s+", " "));
    }
  };
  private static final Factory<StdArrangementRuleAliasToken> FACTORY = () -> new StdArrangementRuleAliasToken("");
  private static final Cloner<StdArrangementRuleAliasToken> CLONER = new Cloner<StdArrangementRuleAliasToken>() {
    @Override
    public StdArrangementRuleAliasToken cloneOf(StdArrangementRuleAliasToken original) {
      return copyOf(original);
    }

    @Override
    public StdArrangementRuleAliasToken copyOf(StdArrangementRuleAliasToken original) {
      return new StdArrangementRuleAliasToken(original.getName(), original.getDefinitionRules());
    }
  };
  private static final Equality<StdArrangementRuleAliasToken> COMPARER = (o1, o2) -> Objects.equals(o1.getId(), o2.getId());

  @NotNull private final Set<String> myUsedTokenIds;
  @NotNull private final ArrangementStandardSettingsManager mySettingsManager;
  @NotNull private final ArrangementColorsProvider myColorsProvider;

  protected ArrangementRuleAliasesListEditor(@NotNull ArrangementStandardSettingsManager settingsManager,
                                             @NotNull ArrangementColorsProvider colorsProvider,
                                             @NotNull List<StdArrangementRuleAliasToken> items,
                                             @NotNull Set<String> usedTokenIds) {
    super(NAMER, FACTORY, CLONER, COMPARER, items, false);
    mySettingsManager = settingsManager;
    myColorsProvider = colorsProvider;
    myUsedTokenIds = usedTokenIds;
    reset();
    initTree();
  }

  @Override
  protected UnnamedConfigurable createConfigurable(StdArrangementRuleAliasToken item) {
    return new ArrangementRuleAliasConfigurable(mySettingsManager, myColorsProvider, item);
  }

  @Override
  protected boolean canDelete(StdArrangementRuleAliasToken item) {
    return !myUsedTokenIds.contains(item.getId());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("configurable.ArrangementRuleAliasesListEditor.display.name");
  }

  @Nullable
  @Override
  public String askForProfileName(String titlePattern) {
    String title = MessageFormat.format(titlePattern, subjDisplayName());
    return Messages.showInputDialog(LangBundle.message("message.new.0.name", subjDisplayName()), title, Messages.getQuestionIcon(), "", new InputValidator() {
      @Override
      public boolean checkInput(String s) {
        return s.length() > 0 && findByName(s) == null && Verifier.checkElementName(s) == null;
      }

      @Override
      public boolean canClose(String s) {
        return checkInput(s);
      }
    });
  }
}
