// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.searcheverywhere.ClassSearchEverywhereContributor;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.playback.commands.ActionCommand;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.InputEvent;

public class GotoClassAction extends SearchEverywhereBaseAction implements DumbAware {

  public GotoClassAction() {
    //we need to change the template presentation to show the proper text for the action in Settings | Keymap
    Presentation presentation = getTemplatePresentation();
    presentation.setText(() -> IdeBundle.message("go.to.class.title.prefix", GotoClassPresentationUpdater.getActionTitle() + "..."));
    presentation.setDescription(() -> IdeBundle.message("go.to.class.action.description",
                                                        StringUtil.join(GotoClassPresentationUpdater.getElementKinds(), "/")));
    addTextOverride(ActionPlaces.MAIN_MENU, () -> GotoClassPresentationUpdater.getActionTitle() + "...");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    boolean dumb = DumbService.isDumb(project);
    if (!dumb || isContributorDumbAware(e)) {
      showInSearchEverywherePopup(ClassSearchEverywhereContributor.class.getSimpleName(), e, true, true);
    }
    else {
      invokeGoToFile(project, e);
    }
  }

  private static boolean isContributorDumbAware(AnActionEvent e) {
    ClassSearchEverywhereContributor contributor = null;
    try {
      contributor = new ClassSearchEverywhereContributor(e);
      return contributor.isDumbAware();
    }
    finally {
      if (contributor != null) Disposer.dispose(contributor);
    }
  }

  static void invokeGoToFile(@NotNull Project project, @NotNull AnActionEvent e) {
    String actionTitle = StringUtil.trimEnd(ObjectUtils.notNull(
      e.getPresentation().getText(), GotoClassPresentationUpdater.getActionTitle()), "...");
    String message = IdeBundle.message("go.to.class.dumb.mode.message", actionTitle);
    DumbService.getInstance(project).showDumbModeNotification(message);
    AnAction action = ActionManager.getInstance().getAction(GotoFileAction.ID);
    InputEvent event = ActionCommand.getInputEvent(GotoFileAction.ID);
    Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    ActionManager.getInstance().tryToExecute(action, event, component, e.getPlace(), true);
  }

  @Override
  protected boolean hasContributors(@NotNull DataContext dataContext) {
    return ChooseByNameRegistry.getInstance().getClassModelContributors().length > 0;
  }
}
