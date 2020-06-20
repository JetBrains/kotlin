/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.find.FindBundle;
import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.find.SearchSession;
import com.intellij.find.impl.RegExHelpPopup;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ex.TooltipLinkProvider;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ToggleRegex extends EditorHeaderToggleAction implements Embeddable, TooltipLinkProvider {
  public ToggleRegex() {
    super(FindBundle.message("find.regex"),
          AllIcons.Actions.Regex,
          AllIcons.Actions.RegexHovered,
          AllIcons.Actions.RegexSelected);
  }

  @Override
  public Pair<@NotNull String, @NotNull Runnable> getTooltipLink(@Nullable JComponent owner) {
    return Pair.create(FindBundle.message("find.regex.help.link"), RegExHelpPopup.createRegExLinkRunnable(owner));
  }

  @Override
  protected boolean isSelected(@NotNull SearchSession session) {
    return session.getFindModel().isRegularExpressions();
  }

  @Override
  protected void setSelected(@NotNull SearchSession session, boolean selected) {
    FindModel findModel = session.getFindModel();
    findModel.setRegularExpressions(selected);
    if (selected) {
      findModel.setWholeWordsOnly(false);
    }
    FindSettings.getInstance().setLocalRegularExpressions(selected);
  }
}
