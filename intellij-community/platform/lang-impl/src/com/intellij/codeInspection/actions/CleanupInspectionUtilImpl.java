package com.intellij.codeInspection.actions;

import com.intellij.codeInspection.BatchQuickFix;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.QuickFix;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.SequentialModalProgressTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class CleanupInspectionUtilImpl implements CleanupInspectionUtil {
  private final static Logger LOG = Logger.getInstance(CleanupInspectionUtilImpl.class);

@Override
  public AbstractPerformFixesTask applyFixesNoSort(@NotNull Project project,
                                                   @NotNull String presentationText,
                                                   @NotNull List<? extends ProblemDescriptor> descriptions,
                                                   @Nullable Class quickfixClass,
                                                   boolean startInWriteAction,
                                                   boolean markGlobal) {
    final boolean isBatch = quickfixClass != null && BatchQuickFix.class.isAssignableFrom(quickfixClass);
    final AbstractPerformFixesTask fixesTask = isBatch ?
        new PerformBatchFixesTask(project, descriptions.toArray(ProblemDescriptor.EMPTY_ARRAY), quickfixClass) :
        new PerformFixesTask(project, descriptions.toArray(ProblemDescriptor.EMPTY_ARRAY), quickfixClass);
    CommandProcessor.getInstance().executeCommand(project, () -> {
      if (markGlobal) CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
      if (quickfixClass != null && startInWriteAction) {
        ((ApplicationImpl)ApplicationManager.getApplication())
            .runWriteActionWithCancellableProgressInDispatchThread(presentationText, project, null, fixesTask::doRun);
      }
      else {
        final SequentialModalProgressTask progressTask =
            new SequentialModalProgressTask(project, presentationText, true);
        progressTask.setMinIterationTime(200);
        progressTask.setTask(fixesTask);
        ProgressManager.getInstance().run(progressTask);
      }
    }, presentationText, null);
    return fixesTask;
  }

  @Override
  public AbstractPerformFixesTask applyFixesNoSort(@NotNull Project project,
                                                   @NotNull String presentationText,
                                                   @NotNull List<? extends ProblemDescriptor> descriptions,
                                                   @Nullable Class quickfixClass,
                                                   boolean startInWriteAction) {
    return applyFixesNoSort(project, presentationText, descriptions, quickfixClass, startInWriteAction, true);
  }

  private static class PerformBatchFixesTask extends AbstractPerformFixesTask {
    private final List<ProblemDescriptor> myBatchModeDescriptors = new ArrayList<>();
    private boolean myApplied = false;

    PerformBatchFixesTask(@NotNull Project project,
                                 @NotNull CommonProblemDescriptor[] descriptors,
                                 @NotNull Class quickfixClass) {
      super(project, descriptors, quickfixClass);
    }

    @Override
    protected void collectFix(QuickFix fix, ProblemDescriptor descriptor, Project project) {
      myBatchModeDescriptors.add(descriptor);
    }

    @Override
    public boolean isDone() {
      if (super.isDone()) {
        if (!myApplied && !myBatchModeDescriptors.isEmpty()) {
          final ProblemDescriptor representative = myBatchModeDescriptors.get(0);
          LOG.assertTrue(representative.getFixes() != null);
          for (QuickFix fix : representative.getFixes()) {
            if (fix != null && fix.getClass().isAssignableFrom(myQuickfixClass)) {
              ((BatchQuickFix)fix).applyFix(myProject,
                  myBatchModeDescriptors.toArray(ProblemDescriptor.EMPTY_ARRAY),
                  new ArrayList<>(),
                  null);
              break;
            }
          }
          myApplied = true;
        }
        return true;
      }
      else {
        return false;
      }
    }
  }

  private static class PerformFixesTask extends AbstractPerformFixesTask {
    PerformFixesTask(@NotNull Project project,
                            @NotNull CommonProblemDescriptor[] descriptors,
                            @Nullable Class quickFixClass) {
      super(project, descriptors, quickFixClass);
    }

    @Override
    protected void collectFix(QuickFix fix, ProblemDescriptor descriptor, Project project) {
      fix.applyFix(project, descriptor);
    }
  }
}
