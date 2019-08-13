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
package com.intellij.find.editorHeaderActions;

import com.intellij.find.EditorSearchSession;
import com.intellij.find.FindModel;
import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class TogglePreserveCaseAction extends EditorHeaderToggleAction {
  public TogglePreserveCaseAction() {
    super("Pr&eserve Case");
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);

    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    FindModel findModel = search != null ? search.getFindModel() : null;
    e.getPresentation().setEnabled(findModel != null && !findModel.isRegularExpressions());
  }

  @Override
  protected boolean isSelected(@NotNull SearchSession session) {
    return session.getFindModel().isPreserveCase();
  }

  @Override
  protected void setSelected(@NotNull SearchSession session, boolean selected) {
    session.getFindModel().setPreserveCase(selected);
  }
}
