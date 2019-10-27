// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testIntegration;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GenerateFromTestCreatorsGroup extends ActionGroup {
  @NotNull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    if (e == null) {
      return AnAction.EMPTY_ARRAY;
    }
    Project project = e.getData(CommonDataKeys.PROJECT);
    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (project == null || file == null) {
      return AnAction.EMPTY_ARRAY;
    }
    List<AnAction> result = new SmartList<>();
    for (TestCreator creator : LanguageTestCreators.INSTANCE.allForLanguage(file.getLanguage())) {
      result.add(new AnAction() {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          creator.createTest(project, editor, file);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
          String text = creator instanceof ItemPresentation ? ((ItemPresentation)creator).getPresentableText() : null;
          Presentation presentation = e.getPresentation();
          presentation.setText(ObjectUtils.notNull(text, "Test..."));
          presentation.setEnabledAndVisible(creator.isAvailable(project, editor, file));
        }

        @Override
        public boolean isDumbAware() {
          return DumbService.isDumbAware(creator);
        }
      });
    }
    return result.toArray(AnAction.EMPTY_ARRAY);
  }
}
