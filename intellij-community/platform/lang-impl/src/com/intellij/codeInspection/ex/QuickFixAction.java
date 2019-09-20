// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ex;

import com.intellij.ReviseWhenPortedToJDK;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefManagerImpl;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.codeInspection.ui.InspectionResultsViewComparator;
import com.intellij.codeInspection.ui.InspectionTree;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.ClickListener;
import com.intellij.ui.ComponentUtil;
import com.intellij.util.SequentialModalProgressTask;
import com.intellij.util.ui.JBUI;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author max
 */
public abstract class QuickFixAction extends AnAction implements CustomComponentAction {
  private static final Logger LOG = Logger.getInstance(QuickFixAction.class);

  public static final QuickFixAction[] EMPTY = new QuickFixAction[0];
  protected final InspectionToolWrapper myToolWrapper;

  protected static InspectionResultsView getInvoker(@NotNull AnActionEvent e) {
    return e.getData(InspectionResultsView.DATA_KEY);
  }

  protected QuickFixAction(String text, @NotNull InspectionToolWrapper toolWrapper) {
    this(text, AllIcons.Actions.IntentionBulb, null, toolWrapper);
  }

  protected QuickFixAction(String text, Icon icon, KeyStroke keyStroke, @NotNull InspectionToolWrapper toolWrapper) {
    super(text, null, icon);
    myToolWrapper = toolWrapper;
    if (keyStroke != null) {
      registerCustomShortcutSet(new CustomShortcutSet(keyStroke), null);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final InspectionResultsView view = getInvoker(e);
    if (view == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    e.getPresentation().setEnabledAndVisible(false);

    final InspectionTree tree = view.getTree();
    final InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper(true);
    if (!view.isSingleToolInSelection() || toolWrapper != myToolWrapper) {
      return;
    }

    if (!isProblemDescriptorsAcceptable() && tree.getSelectedElements().length > 0 ||
        isProblemDescriptorsAcceptable() && tree.getSelectedDescriptors().length > 0) {
      e.getPresentation().setEnabledAndVisible(true);
    }
  }

  protected boolean isProblemDescriptorsAcceptable() {
    return false;
  }

  public String getText() {
    return getTemplatePresentation().getText();
  }

  @Override
  @ReviseWhenPortedToJDK("9")
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final InspectionResultsView view = getInvoker(e);
    final InspectionTree tree = view.getTree();
    try {
      Ref<List<CommonProblemDescriptor[]>> descriptors = Ref.create();
      Set<VirtualFile> readOnlyFiles = new THashSet<>();
      TreePath[] paths = tree.getSelectionPaths();
      if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> ReadAction.run(() -> {
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        indicator.setText("Checking problem descriptors...");
        descriptors.set(tree.getSelectedDescriptorPacks(true, readOnlyFiles, false, paths));
      }), InspectionsBundle.message("preparing.for.apply.fix"), true, e.getProject())) {
        return;
      }
      if (isProblemDescriptorsAcceptable() && descriptors.get().size() > 0) {
        doApplyFix(view.getProject(), descriptors.get(), readOnlyFiles, tree.getContext());
      } else {
        doApplyFix(getSelectedElements(view), view);
      }

      view.getTree().removeSelectedProblems();
    } finally {
      view.setApplyingFix(false);
    }
  }


  protected void applyFix(@NotNull Project project,
                          @NotNull GlobalInspectionContextImpl context,
                          @NotNull CommonProblemDescriptor[] descriptors,
                          @NotNull Set<? super PsiElement> ignoredElements) {
  }

  private void doApplyFix(@NotNull Project project,
                          @NotNull List<CommonProblemDescriptor[]> descriptors,
                          @NotNull Set<? extends VirtualFile> readOnlyFiles,
                          @NotNull GlobalInspectionContextImpl context) {
    if (!FileModificationService.getInstance().prepareVirtualFilesForWrite(project, readOnlyFiles)) return;

    final RefManagerImpl refManager = (RefManagerImpl)context.getRefManager();
    final boolean initial = refManager.isInProcess();

    refManager.inspectionReadActionFinished();

    try {
      final Set<PsiElement> resolvedElements = new HashSet<>();
      performFixesInBatch(project, descriptors, context, resolvedElements);

      refreshViews(project, resolvedElements, myToolWrapper);
    }
    finally { //to make offline view lazy
      if (initial) refManager.inspectionReadActionStarted();
    }
  }

  protected boolean startInWriteAction() {
    return false;
  }

  protected void performFixesInBatch(@NotNull Project project,
                                     @NotNull List<CommonProblemDescriptor[]> descriptors,
                                     @NotNull GlobalInspectionContextImpl context,
                                     Set<? super PsiElement> ignoredElements) {
    final String templatePresentationText = getTemplatePresentation().getText();
    assert templatePresentationText != null;
    CommandProcessor.getInstance().executeCommand(project, () -> {
      CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
      boolean startInWriteAction = startInWriteAction();
      PerformFixesTask performFixesTask = new PerformFixesTask(project, descriptors, ignoredElements, context);
      if (startInWriteAction) {
        ((ApplicationImpl)ApplicationManager.getApplication())
          .runWriteActionWithCancellableProgressInDispatchThread(templatePresentationText, project, null, performFixesTask::doRun);
      }
      else {
        final SequentialModalProgressTask progressTask =
          new SequentialModalProgressTask(project, templatePresentationText, true);
        progressTask.setMinIterationTime(200);
        progressTask.setTask(performFixesTask);
        ProgressManager.getInstance().run(progressTask);
      }
    }, templatePresentationText, null);
  }

  private void doApplyFix(@NotNull final RefEntity[] refElements, @NotNull InspectionResultsView view) {
    final RefManagerImpl refManager = (RefManagerImpl)view.getGlobalInspectionContext().getRefManager();

    final boolean initial = refManager.isInProcess();

    refManager.inspectionReadActionFinished();

    try {
      final boolean[] refreshNeeded = {false};
      if (refElements.length > 0) {
        final Project project = refElements[0].getRefManager().getProject();
        CommandProcessor.getInstance().executeCommand(project, () -> {
          CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
          ApplicationManager.getApplication().runWriteAction(() -> {
            refreshNeeded[0] = applyFix(refElements);
          });
        }, getTemplatePresentation().getText(), null);
      }
      if (refreshNeeded[0]) {
        refreshViews(view.getProject(), refElements, myToolWrapper);
      }
    }
    finally {  //to make offline view lazy
      if (initial) refManager.inspectionReadActionStarted();
    }
  }

  public static void removeElements(@NotNull RefEntity[] refElements, @NotNull Project project, @NotNull InspectionToolWrapper toolWrapper) {
    refreshViews(project, refElements, toolWrapper);
    final ArrayList<RefElement> deletedRefs = new ArrayList<>(1);
    for (RefEntity refElement : refElements) {
      if (!(refElement instanceof RefElement)) continue;
      refElement.getRefManager().removeRefElement((RefElement)refElement, deletedRefs);
    }
  }

  private static Set<VirtualFile> getReadOnlyFiles(@NotNull RefEntity[] refElements) {
    Set<VirtualFile> readOnlyFiles = new THashSet<>();
    for (RefEntity refElement : refElements) {
      PsiElement psiElement = refElement instanceof RefElement ? ((RefElement)refElement).getPsiElement() : null;
      if (psiElement == null || psiElement.getContainingFile() == null) continue;
      readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
    }
    return readOnlyFiles;
  }

  @NotNull
  private static RefEntity[] getSelectedElements(InspectionResultsView view) {
    if (view == null) return RefEntity.EMPTY_ELEMENTS_ARRAY;
    RefEntity[] selection = view.getTree().getSelectedElements();
    PsiDocumentManager.getInstance(view.getProject()).commitAllDocuments();
    Arrays.sort(selection, InspectionResultsViewComparator::compareEntities);
    return selection;
  }

  private static void refreshViews(@NotNull Project project, @NotNull Set<? extends PsiElement> resolvedElements, @NotNull InspectionToolWrapper toolWrapper) {
    InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final Set<GlobalInspectionContextImpl> runningContexts = managerEx.getRunningContexts();
    for (GlobalInspectionContextImpl context : runningContexts) {
      for (PsiElement element : resolvedElements) {
        context.resolveElement(toolWrapper.getTool(), element);
      }
      context.refreshViews();
    }
  }

  protected static void refreshViews(@NotNull Project project, @NotNull RefEntity[] resolvedElements, @NotNull InspectionToolWrapper toolWrapper) {
    final Set<PsiElement> ignoredElements = new HashSet<>();
    for (RefEntity element : resolvedElements) {
      final PsiElement psiElement = element instanceof RefElement ? ((RefElement)element).getPsiElement() : null;
      if (psiElement != null && psiElement.isValid()) {
        ignoredElements.add(psiElement);
      }
    }
    refreshViews(project, ignoredElements, toolWrapper);
  }

  /**
   * @return true if immediate UI update needed.
   */
  protected boolean applyFix(@NotNull RefEntity[] refElements) {
    Set<VirtualFile> readOnlyFiles = getReadOnlyFiles(refElements);
    if (!readOnlyFiles.isEmpty()) {
      final Project project = refElements[0].getRefManager().getProject();
      final ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(readOnlyFiles);
      if (operationStatus.hasReadonlyFiles()) return false;
    }
    return true;
  }

  @NotNull
  @Override
  public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
    final JButton button = new JButton(presentation.getText());
    Icon icon = presentation.getIcon();
    if (icon == null) {
      icon = AllIcons.Actions.IntentionBulb;
    }
    button.setEnabled(presentation.isEnabled());
    button.setIcon(IconLoader.getTransparentIcon(icon, 0.75f));
    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent event, int clickCount) {
        final ActionToolbar toolbar = ComponentUtil.getParentOfType((Class<? extends ActionToolbar>)ActionToolbar.class, (Component)button);
        actionPerformed(AnActionEvent.createFromAnAction(QuickFixAction.this,
                                                         event,
                                                         place,
                                                         toolbar == null ? DataManager.getInstance().getDataContext(button) : toolbar.getToolbarDataContext()));
        return true;
      }
    }.installOn(button);
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
    panel.setBorder(JBUI.Borders.empty(7, 0, 8, 0));
    panel.add(button);
    return panel;
  }

  private class PerformFixesTask extends PerformFixesModalTask {
    @NotNull private final GlobalInspectionContextImpl myContext;
    @NotNull
    private final Set<? super PsiElement> myIgnoredElements;

    PerformFixesTask(@NotNull Project project,
                     @NotNull List<CommonProblemDescriptor[]> descriptors,
                     @NotNull Set<? super PsiElement> ignoredElements,
                     @NotNull GlobalInspectionContextImpl context) {
      super(project, descriptors);
      myContext = context;
      myIgnoredElements = ignoredElements;
    }

    @Override
    protected void applyFix(Project project, CommonProblemDescriptor descriptor) {
      if (descriptor instanceof ProblemDescriptor &&
          ((ProblemDescriptor)descriptor).getStartElement() == null &&
          ((ProblemDescriptor)descriptor).getEndElement() == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Invalidated psi for " + descriptor);
        }
        return;
      }

      try {
        QuickFixAction.this.applyFix(myProject, myContext, new CommonProblemDescriptor[]{descriptor}, myIgnoredElements);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }
}
