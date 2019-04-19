package com.intellij.execution.runners;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Reruns all registered execution sessions.<p>
 * The difference between this action and {@code Rerun} action (Ctrl+F5) is that this action reruns
 * only explicitly registered execution sessions. For example, their tabs can be hidden by other tabs, it doesn't matter.
 * <p>
 * Thus it can be convenient for rerunning tests, because running non-test execution session after
 * running test execution session won't hide the latter.
 *
 * @see RerunTestsNotification
 */
public class RerunTestsAction extends DumbAwareAction implements AnAction.TransparentUpdate {
  public static final String ID = "RerunTests";
  private static final Set<RunContentDescriptor> REGISTRY = ContainerUtil.newHashSet();

  public static void register(@NotNull final RunContentDescriptor descriptor) {
    if (!Disposer.isDisposed(descriptor) && REGISTRY.add(descriptor)) {
      Disposer.register(descriptor, new Disposable() {
        @Override
        public void dispose() {
          REGISTRY.remove(descriptor);
        }
      });
    }
  }

  /**
   * @deprecated use {@link #register(RunContentDescriptor)} instead
   */
  @Deprecated
  public static void register(@NotNull ExecutionEnvironment environment) {
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    List<RunContentDescriptor> descriptors = ContainerUtil.newArrayList(REGISTRY);
    for (RunContentDescriptor descriptor : descriptors) {
      if (Disposer.isDisposed(descriptor)) {
        REGISTRY.remove(descriptor);
      }
      else {
        Project project = e.getProject();
        if (project != null) {
          RunContentManager runContentManager = ExecutionManager.getInstance(project).getContentManager();
          // check if the descriptor belongs to the current project
          if (runContentManager.getToolWindowByDescriptor(descriptor) != null) {
            ExecutionUtil.restart(descriptor);
          }
        }
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getProject() != null);
  }
}
