// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.core.CoreBundle;
import com.intellij.lang.LanguageFormatting;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SequentialTask;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.diff.FilesTooBigForDiffException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public abstract class AbstractLayoutCodeProcessor {
  private static final Logger LOG = Logger.getInstance(AbstractLayoutCodeProcessor.class);

  @NotNull
  protected final Project myProject;
  private final Module myModule;

  private PsiDirectory myDirectory;
  private PsiFile myFile;
  private List<PsiFile> myFiles;
  private boolean myIncludeSubdirs;

  private final String myProgressText;
  private final String myCommandName;
  private Runnable myPostRunnable;
  private boolean myProcessChangedTextOnly;

  protected AbstractLayoutCodeProcessor myPreviousCodeProcessor;
  private List<VirtualFileFilter> myFilters = new ArrayList<>();

  private LayoutCodeInfoCollector myInfoCollector;

  protected AbstractLayoutCodeProcessor(@NotNull Project project, String commandName, String progressText, boolean processChangedTextOnly) {
    this(project, (Module)null, commandName, progressText, processChangedTextOnly);
  }

  protected AbstractLayoutCodeProcessor(@NotNull AbstractLayoutCodeProcessor previous,
                                        @NotNull String commandName,
                                        @NotNull String progressText) {
    myProject = previous.myProject;
    myModule = previous.myModule;
    myDirectory = previous.myDirectory;
    myFile = previous.myFile;
    myFiles = previous.myFiles;
    myIncludeSubdirs = previous.myIncludeSubdirs;
    myProcessChangedTextOnly = previous.myProcessChangedTextOnly;

    myPostRunnable = null;
    myProgressText = progressText;
    myCommandName = commandName;
    myPreviousCodeProcessor = previous;
    myFilters = previous.myFilters;
    myInfoCollector = previous.myInfoCollector;
  }

  protected AbstractLayoutCodeProcessor(@NotNull Project project,
                                        @Nullable Module module,
                                        String commandName,
                                        String progressText,
                                        boolean processChangedTextOnly) {
    myProject = project;
    myModule = module;
    myDirectory = null;
    myIncludeSubdirs = true;
    myCommandName = commandName;
    myProgressText = progressText;
    myPostRunnable = null;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  protected AbstractLayoutCodeProcessor(@NotNull Project project,
                                        @NotNull PsiDirectory directory,
                                        boolean includeSubdirs,
                                        String progressText,
                                        String commandName,
                                        boolean processChangedTextOnly) {
    myProject = project;
    myModule = null;
    myDirectory = directory;
    myIncludeSubdirs = includeSubdirs;
    myProgressText = progressText;
    myCommandName = commandName;
    myPostRunnable = null;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  protected AbstractLayoutCodeProcessor(@NotNull Project project,
                                        @NotNull PsiFile file,
                                        String progressText,
                                        String commandName,
                                        boolean processChangedTextOnly) {
    myProject = project;
    myModule = null;
    myFile = file;
    myProgressText = progressText;
    myCommandName = commandName;
    myPostRunnable = null;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  protected AbstractLayoutCodeProcessor(@NotNull Project project,
                                        PsiFile @NotNull [] files,
                                        String progressText,
                                        String commandName,
                                        @Nullable Runnable postRunnable,
                                        boolean processChangedTextOnly) {
    myProject = project;
    myModule = null;
    myFiles = ContainerUtil.filter(files, AbstractLayoutCodeProcessor::canBeFormatted);
    myProgressText = progressText;
    myCommandName = commandName;
    myPostRunnable = postRunnable;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  public void setPostRunnable(Runnable postRunnable) {
    myPostRunnable = postRunnable;
  }

  public void setCollectInfo(boolean isCollectInfo) {
    myInfoCollector = isCollectInfo ? new LayoutCodeInfoCollector() : null;

    AbstractLayoutCodeProcessor current = this;
    while (current.myPreviousCodeProcessor != null) {
      current = current.myPreviousCodeProcessor;
      current.myInfoCollector = myInfoCollector;
    }
  }

  public void addFileFilter(@NotNull VirtualFileFilter filter) {
    myFilters.add(filter);
  }

  void setProcessChangedTextOnly(boolean value) {
    myProcessChangedTextOnly = value;
  }

  /**
   * Ensures that given file is ready to reformatting and prepares it if necessary.
   *
   * @param file                    file to process
   * @param processChangedTextOnly  flag that defines is only the changed text (in terms of VCS change) should be processed
   * @return          task that triggers formatting of the given file. Returns value of that task indicates whether formatting
   *                  is finished correctly or not (exception occurred, user cancelled formatting etc)
   * @throws IncorrectOperationException    if unexpected exception occurred during formatting
   */
  @NotNull
  protected abstract FutureTask<Boolean> prepareTask(@NotNull PsiFile file, boolean processChangedTextOnly) throws IncorrectOperationException;

  /**
   * @deprecated This method incorrectly combines several {@link #prepareTask} results,
   * so that some of them might get outdated after previous results are executed in write action.
   * Use {@link #run()} or {@link #runWithoutProgress()} instead.
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
  public FutureTask<Boolean> preprocessFile(@NotNull PsiFile file, boolean processChangedTextOnly) throws IncorrectOperationException {
    final FutureTask<Boolean> previousTask =
      myPreviousCodeProcessor != null ? myPreviousCodeProcessor.preprocessFile(file, processChangedTextOnly)
                                      : null;
    final FutureTask<Boolean> currentTask = prepareTask(file, processChangedTextOnly);

    return new FutureTask<>(() -> {
      try {
        if (previousTask != null) {
          previousTask.run();
          if (!previousTask.get() || previousTask.isCancelled()) return false;
        }

        ApplicationManager.getApplication().runWriteAction(currentTask);

        return currentTask.get() && !currentTask.isCancelled();
      }
      catch (ExecutionException e) {
        ExceptionUtil.rethrowUnchecked(e.getCause());
        throw e;
      }
    });
  }

  public void run() {
    if (myFile != null) {
      runProcessFile(myFile);
      return;
    }

    runProcessFiles();
  }

  @NotNull
  private FileRecursiveIterator build() {
    if (myFiles != null) {
      return new FileRecursiveIterator(myProject, myFiles);
    }
    if (myProcessChangedTextOnly) {
      return buildChangedFilesIterator();
    }
    if (myDirectory != null) {
      return new FileRecursiveIterator(myDirectory);
    }
    if (myModule != null) {
      return new FileRecursiveIterator(myModule);
    }
    return new FileRecursiveIterator(myProject);
  }

  @NotNull
  private FileRecursiveIterator buildChangedFilesIterator() {
    List<PsiFile> files = getChangedFilesFromContext();
    return new FileRecursiveIterator(myProject, files);
  }

  @NotNull
  private List<PsiFile> getChangedFilesFromContext() {
    List<PsiDirectory> dirs = getAllSearchableDirsFromContext();
    return VcsFacade.getInstance().getChangedFilesFromDirs(myProject, dirs);
  }

  private List<PsiDirectory> getAllSearchableDirsFromContext() {
    List<PsiDirectory> dirs = new ArrayList<>();
    if (myDirectory != null) {
      dirs.add(myDirectory);
    }
    else if (myModule != null) {
      List<PsiDirectory> allModuleDirs = FileRecursiveIterator.collectModuleDirectories(myModule);
      dirs.addAll(allModuleDirs);
    }
    else {
      List<PsiDirectory> allProjectDirs = FileRecursiveIterator.collectProjectDirectories(myProject);
      dirs.addAll(allProjectDirs);
    }
    return dirs;
  }


  private void runProcessFile(@NotNull final PsiFile file) {
    PsiUtilCore.ensureValid(file);

    Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);

    if (document == null) {
      return;
    }

    if (!FileDocumentManager.getInstance().requestWriting(document, myProject)) {
      Messages.showMessageDialog(myProject, CoreBundle.message("cannot.modify.a.read.only.file", file.getName()),
                                 CodeInsightBundle.message("error.dialog.readonly.file.title"),
                                 Messages.getErrorIcon()
      );
      return;
    }

    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, myCommandName, true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText(myProgressText);
        try {
          new ReformatFilesTask(indicator).performFileProcessing(file);
        }
        catch(IndexNotReadyException e) {
          LOG.warn(e);
          return;
        }
        if (myPostRunnable != null) {
          ApplicationManager.getApplication().invokeLater(myPostRunnable);
        }
      }
    });
  }

  private void runProcessFiles() {
    boolean isSuccess = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      return processAllUnderProgress(indicator);
    }, myCommandName, true, myProject);

    if (isSuccess && myPostRunnable != null) {
      myPostRunnable.run();
    }
  }

  private static boolean canBeFormatted(@NotNull PsiFile file) {
    if (!file.isValid()) return false;
    if (LanguageFormatting.INSTANCE.forContext(file) == null) {
      return false;
    }
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return true;

    if (ProjectUtil.isProjectOrWorkspaceFile(virtualFile)) return false;

    return !GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(virtualFile, file.getProject());
  }

  public void runWithoutProgress() throws IncorrectOperationException {
    new ReformatFilesTask(new EmptyProgressIndicator()).performFileProcessing(myFile);
  }

  public boolean processAllUnderProgress(ProgressIndicator indicator) {
    indicator.setIndeterminate(false);
    ReformatFilesTask task = new ReformatFilesTask(indicator);
    return task.process();
  }

  private @NotNull List<AbstractLayoutCodeProcessor> getAllProcessors() {
    AbstractLayoutCodeProcessor current = this;
    List<AbstractLayoutCodeProcessor> all = new ArrayList<>();
    while (current != null) {
      all.add(current);
      current = current.myPreviousCodeProcessor;
    }
    Collections.reverse(all);
    return all;
  }

  private class ReformatFilesTask implements SequentialTask {
    private final List<AbstractLayoutCodeProcessor> myProcessors;

    private final FileRecursiveIterator myFileTreeIterator;
    private final FileRecursiveIterator myCountingIterator;

    private final ProgressIndicator myProgressIndicator;

    private int myTotalFiles;
    private int myFilesProcessed;
    private boolean myStopFormatting;
    private PsiFile next;

    ReformatFilesTask(@NotNull ProgressIndicator indicator) {
      myFileTreeIterator = ReadAction.compute(() -> build());
      myCountingIterator = ReadAction.compute(() -> build());
      myProcessors = getAllProcessors();
      myProgressIndicator = indicator;
    }

    @Override
    public boolean isDone() {
      return myStopFormatting;
    }

    private void countingIteration() {
      myTotalFiles++;
    }

    @Override
    public boolean iteration() {
      if (myStopFormatting) {
        return true;
      }

      updateIndicatorFraction(myFilesProcessed);

      if (next != null) {
        PsiFile file = next;
        myFilesProcessed++;

        if (shouldProcessFile(file)) {
          updateIndicatorText(ApplicationBundle.message("bulk.reformat.process.progress.text"), getPresentablePath(file));
          DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> performFileProcessing(file));
        }
      }

      return true;
    }

    private Boolean shouldProcessFile(PsiFile file) {
      return ReadAction.compute(() -> file.isWritable() && canBeFormatted(file) && acceptedByFilters(file));
    }

    private void performFileProcessing(@NotNull PsiFile file) {
      String groupId = AbstractLayoutCodeProcessor.this.toString();
      for (AbstractLayoutCodeProcessor processor : myProcessors) {
        FutureTask<Boolean> writeTask = ReadAction.compute(() -> processor.prepareTask(file, myProcessChangedTextOnly));

        ProgressIndicatorProvider.checkCanceled();

        ApplicationManager.getApplication().invokeAndWait(
          () -> WriteCommandAction.runWriteCommandAction(myProject, myCommandName, groupId, writeTask));

        checkStop(writeTask, file);
      }
    }

    private void checkStop(FutureTask<Boolean> task, PsiFile file) {
      try {
        if (!task.get() || task.isCancelled()) {
          myStopFormatting = true;
        }
      }
      catch (InterruptedException | ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IndexNotReadyException) {
          LOG.warn(cause);
          return;
        }
        LOG.error("Got unexpected exception during formatting " + file, e);
      }
    }

    private void updateIndicatorText(@NotNull String upperLabel, @NotNull String downLabel) {
      myProgressIndicator.setText(upperLabel);
      myProgressIndicator.setText2(downLabel);
    }

    private String getPresentablePath(@NotNull PsiFile file) {
      VirtualFile vFile = file.getVirtualFile();
      return vFile != null ? ProjectUtil.calcRelativeToProjectPath(vFile, myProject) : file.getName();
    }

    private void updateIndicatorFraction(int processed) {
      myProgressIndicator.setFraction((double)processed / myTotalFiles);
    }

    @Override
    public void stop() {
      myStopFormatting = true;
    }

    private boolean process() {
      myCountingIterator.processAll(file -> {
        updateIndicatorText(ApplicationBundle.message("bulk.reformat.prepare.progress.text"), "");
        countingIteration();
        return !isDone();
      });

      return myFileTreeIterator.processAll(file -> {
        next = file;
        iteration();
        return !isDone();
      });
    }
  }

  private boolean acceptedByFilters(@NotNull PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) {
      return false;
    }

    for (VirtualFileFilter filter : myFilters) {
      if (!filter.accept(file.getVirtualFile())) {
        return false;
      }
    }

    return true;
  }

  static List<TextRange> getSelectedRanges(@NotNull SelectionModel selectionModel) {
    final List<TextRange> ranges = new SmartList<>();
    if (selectionModel.hasSelection()) {
      TextRange range = TextRange.create(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
      ranges.add(range);
    }
    return ranges;
  }

  void handleFileTooBigException(Logger logger, FilesTooBigForDiffException e, @NotNull PsiFile file) {
    logger.info("Error while calculating changed ranges for: " + file.getVirtualFile(), e);
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      Notification notification = new Notification(NotificationGroup.createIdWithTitle("Reformat changed text", ApplicationBundle.message("reformat.changed.text.file.too.big.notification.groupId")),
                                                   ApplicationBundle.message("reformat.changed.text.file.too.big.notification.title"),
                                                   ApplicationBundle.message("reformat.changed.text.file.too.big.notification.text", file.getName()),
                                                   NotificationType.INFORMATION);
      notification.notify(file.getProject());
    }
  }

  @Nullable
  public LayoutCodeInfoCollector getInfoCollector() {
    return myInfoCollector;
  }
}
