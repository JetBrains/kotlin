package com.intellij.platform.renameProject;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author lene
 */
public class RenameProjectAction extends DumbAwareAction {

  public RenameProjectAction() {
    super(RefactoringBundle.message("rename.project.action.title"), RefactoringBundle.message("renames.project"), null);
  }

  private static final Logger LOG = Logger.getInstance(RenameProjectAction.class);

  @Override
  public void update(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    e.getPresentation().setEnabled(project != null && !project.isDefault());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    LOG.assertTrue(project instanceof ProjectEx);
    final Module[] modules = ModuleManager.getInstance(project).getModules();
    final Module module = ContainerUtil.find(modules, module1 -> project.getName().equals(module1.getName()));
    Messages.showInputDialog(project, RefactoringBundle.message("enter.new.project.name"), RefactoringBundle.message("rename.project"),
                             Messages.getQuestionIcon(),
                             project.getName(),
                             new RenameProjectHandler.MyInputValidator((ProjectEx)project, module));
  }
}
