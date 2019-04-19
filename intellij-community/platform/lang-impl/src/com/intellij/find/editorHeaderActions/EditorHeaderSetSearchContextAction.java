/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.find.FindModel;
import com.intellij.find.SearchSession;
import org.jetbrains.annotations.NotNull;

public class EditorHeaderSetSearchContextAction extends EditorHeaderToggleAction {
  private final FindModel.SearchContext myContext;

  protected EditorHeaderSetSearchContextAction(@NotNull String text, @NotNull FindModel.SearchContext context) {
    super(text);

    myContext = context;
  }

  @Override
  protected boolean isSelected(@NotNull SearchSession session) {
    return session.getFindModel().getSearchContext() == myContext;
  }

  @Override
  protected void setSelected(@NotNull SearchSession session, boolean selected) {
    session.getFindModel().setSearchContext(selected ? myContext : FindModel.SearchContext.ANY);
  }
}
