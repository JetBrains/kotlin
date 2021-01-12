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
package com.intellij.ide.actions;

import com.intellij.ide.actions.searcheverywhere.FileSearchEverywhereContributor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * "Go to | File" action implementation.
 *
 * @author Eugene Belyaev
 * @author Constantine.Plotnikov
 */
public class GotoFileAction extends SearchEverywhereBaseAction implements DumbAware {
  public static final String ID = "GotoFile";

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    showInSearchEverywherePopup(FileSearchEverywhereContributor.class.getSimpleName(), e, true, true);
  }
}
