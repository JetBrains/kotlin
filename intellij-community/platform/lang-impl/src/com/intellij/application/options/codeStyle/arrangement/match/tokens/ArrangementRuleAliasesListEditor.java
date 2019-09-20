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
package com.intellij.application.options.codeStyle.arrangement.match.tokens;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NamedItemsListEditor;
import com.intellij.openapi.ui.Namer;
import com.intellij.openapi.util.Cloner;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import gnu.trove.Equality;
import org.jdom.Verifier;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

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
  private static final Equality<StdArrangementRuleAliasToken> COMPARER = (o1, o2) -> Comparing.equal(o1.getId(), o2.getId());

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
    return "Custom Composite Tokens";
  }

  @Nullable
  @Override
  public String askForProfileName(String titlePattern) {
    String title = MessageFormat.format(titlePattern, subjDisplayName());
    return Messages.showInputDialog("New " + subjDisplayName() + " name:", title, Messages.getQuestionIcon(), "", new InputValidator() {
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
