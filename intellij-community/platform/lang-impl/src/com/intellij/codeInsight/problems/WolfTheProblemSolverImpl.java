// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.problems;

import com.intellij.codeInsight.daemon.impl.*;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.*;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class WolfTheProblemSolverImpl extends WolfTheProblemSolver implements Disposable {
  private final Map<VirtualFile, ProblemFileInfo> myProblems = new THashMap<>(); // guarded by myProblems
  private final Map<VirtualFile, Set<Object>> myProblemsFromExternalSources = new THashMap<>(); // guarded by myProblemsFromExternalSources
  private final Collection<VirtualFile> myCheckingQueue = new THashSet<>(10);

  private final Project myProject;

  WolfTheProblemSolverImpl(@NotNull Project project) {
    myProject = project;
    PsiTreeChangeListener changeListener = new PsiTreeChangeAdapter() {
      @Override
      public void childAdded(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childMoved(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        clearSyntaxErrorFlag(event);
      }
    };
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(changeListener, this);
    MessageBusConnection busConnection = project.getMessageBus().connect();
    busConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        boolean dirChanged = false;
        Set<VirtualFile> toRemove = new THashSet<>();
        for (VFileEvent event : events) {
          if (event instanceof VFileDeleteEvent || event instanceof VFileMoveEvent) {
            VirtualFile file = event.getFile();
            if (file.isDirectory()) {
              dirChanged = true;
            }
            else {
              toRemove.add(file);
            }
          }
        }
        if (dirChanged) {
          clearInvalidFiles();
        }
        for (VirtualFile file : toRemove) {
          doRemove(file);
        }
      }
    });
    FileStatusManager fileStatusManager = FileStatusManager.getInstance(myProject);
    if (fileStatusManager != null) { //tests?
      fileStatusManager.addFileStatusListener(new FileStatusListener() {
        @Override
        public void fileStatusesChanged() {
          clearInvalidFiles();
        }

        @Override
        public void fileStatusChanged(@NotNull VirtualFile virtualFile) {
          fileStatusesChanged();
        }
      }, this);
    }

    busConnection.subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
      @Override
      public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        // Ensure we don't have any leftover problems referring to classes from plugin being unloaded
        Set<VirtualFile> allFiles = new HashSet<>(myProblems.keySet());
        for (VirtualFile file : allFiles) {
          doRemove(file);
        }
      }
    });
  }

  @Override
  public void dispose() {

  }

  private void doRemove(@NotNull VirtualFile problemFile) {
    ProblemFileInfo old;
    synchronized (myProblems) {
      old = myProblems.remove(problemFile);
    }
    synchronized (myCheckingQueue) {
      myCheckingQueue.remove(problemFile);
    }
    if (old != null) {
      // firing outside lock
      if (hasProblemsFromExternalSources(problemFile)) {
        fireProblemsChanged(problemFile);
      }
      else {
        fireProblemsDisappeared(problemFile);
      }
    }
  }

  private static class ProblemFileInfo {
    private final Collection<Problem> problems = new THashSet<>();
    private boolean hasSyntaxErrors;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ProblemFileInfo that = (ProblemFileInfo)o;

      return hasSyntaxErrors == that.hasSyntaxErrors && problems.equals(that.problems);
    }

    @Override
    public int hashCode() {
      int result = problems.hashCode();
      result = 31 * result + (hasSyntaxErrors ? 1 : 0);
      return result;
    }
  }

  private void clearInvalidFiles() {
    clearInvalidFilesFrom(myProblems);
    clearInvalidFilesFrom(myProblemsFromExternalSources);
  }

  private void clearInvalidFilesFrom(Map<VirtualFile, ?> problems) {
    VirtualFile[] files;
    synchronized (problems) {
      files = VfsUtilCore.toVirtualFileArray(problems.keySet());
    }
    for (VirtualFile problemFile : files) {
      if (!problemFile.isValid() || !isToBeHighlighted(problemFile)) {
        doRemove(problemFile);
      }
    }
  }

  private void clearSyntaxErrorFlag(@NotNull PsiTreeChangeEvent event) {
    PsiFile file = event.getFile();
    if (file == null) return;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return;
    synchronized (myProblems) {
      ProblemFileInfo info = myProblems.get(virtualFile);
      if (info != null) {
        info.hasSyntaxErrors = false;
      }
    }
  }

  public void startCheckingIfVincentSolvedProblemsYet(@NotNull ProgressIndicator progress,
                                                      @NotNull ProgressableTextEditorHighlightingPass pass)
    throws ProcessCanceledException {
    if (!myProject.isOpen()) return;

    List<VirtualFile> files;
    synchronized (myCheckingQueue) {
      files = new ArrayList<>(myCheckingQueue);
    }
    // (rough approx number of PSI elements = file length/2) * (visitor count = 2 usually)
    long progressLimit = files.stream().filter(VirtualFile::isValid).mapToLong(VirtualFile::getLength).sum();
    pass.setProgressLimit(progressLimit);
    for (VirtualFile virtualFile : files) {
      progress.checkCanceled();
      if (virtualFile == null) break;
      if (!virtualFile.isValid() || orderVincentToCleanTheCar(virtualFile, progress)) {
        doRemove(virtualFile);
      }
      if (virtualFile.isValid()) {
        pass.advanceProgress(virtualFile.getLength());
      }
    }
  }

  // returns true if car has been cleaned
  private boolean orderVincentToCleanTheCar(@NotNull VirtualFile file,
                                            @NotNull ProgressIndicator progressIndicator) throws ProcessCanceledException {
    if (!isToBeHighlighted(file)) {
      clearProblems(file);
      return true; // file is going to be red waved no more
    }
    if (hasSyntaxErrors(file)) {
      // optimization: it's no use anyway to try clean the file with syntax errors, only changing the file itself can help
      return false;
    }
    if (myProject.isDisposed()) return false;
    if (willBeHighlightedAnyway(file)) return false;
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
    if (psiFile == null) return false;
    Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) return false;

    AtomicReference<HighlightInfo> error = new AtomicReference<>();
    AtomicBoolean hasErrorElement = new AtomicBoolean();
    try {
      GeneralHighlightingPass pass = new GeneralHighlightingPass(myProject, psiFile, document, 0, document.getTextLength(),
                                                                 false, new ProperTextRange(0, document.getTextLength()), null, HighlightInfoProcessor.getEmpty()) {
        @NotNull
        @Override
        protected HighlightInfoHolder createInfoHolder(@NotNull PsiFile file) {
          return new HighlightInfoHolder(file) {
            @Override
            public boolean add(@Nullable HighlightInfo info) {
              if (info != null && info.getSeverity() == HighlightSeverity.ERROR) {
                error.set(info);
                hasErrorElement.set(myHasErrorElement);
                throw new ProcessCanceledException();
              }
              return super.add(info);
            }
          };
        }
      };
      pass.collectInformation(progressIndicator);
    }
    catch (ProcessCanceledException e) {
      if (error.get() != null) {
        ProblemImpl problem = new ProblemImpl(file, error.get(), hasErrorElement.get());
        reportProblems(file, Collections.singleton(problem));
      }
      return false;
    }
    clearProblems(file);
    return true;
  }

  @Override
  public boolean hasSyntaxErrors(@NotNull VirtualFile file) {
    synchronized (myProblems) {
      ProblemFileInfo info = myProblems.get(file);
      return info != null && info.hasSyntaxErrors;
    }
  }

  private boolean willBeHighlightedAnyway(@NotNull VirtualFile file) {
    // opened in some editor, and hence will be highlighted automatically sometime later
    FileEditor[] selectedEditors = FileEditorManager.getInstance(myProject).getSelectedEditors();
    for (FileEditor editor : selectedEditors) {
      if (!(editor instanceof TextEditor)) continue;
      Document document = ((TextEditor)editor).getEditor().getDocument();
      PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(document);
      if (psiFile == null) continue;
      if (Comparing.equal(file, psiFile.getVirtualFile())) return true;
    }
    return false;
  }

  @Override
  public boolean hasProblemFilesBeneath(@NotNull Condition<? super VirtualFile> condition) {
    if (!myProject.isOpen()) return false;
    return checkProblemFilesInMap(condition, myProblems) ||
           checkProblemFilesInMap(condition, myProblemsFromExternalSources);
  }

  private static boolean checkProblemFilesInMap(@NotNull Condition<? super VirtualFile> condition,
                                                @NotNull Map<VirtualFile, ?> map) {
    synchronized (map) {
      if (!map.isEmpty()) {
        for (VirtualFile problemFile : map.keySet()) {
          if (problemFile.isValid() && condition.value(problemFile)) return true;
        }
      }
      return false;
    }
  }

  @Override
  public boolean hasProblemFilesBeneath(@NotNull Module scope) {
    return hasProblemFilesBeneath(virtualFile -> ModuleUtilCore.moduleContainsFile(scope, virtualFile, false));
  }

  @Override
  public void addProblemListener(@NotNull WolfTheProblemSolver.ProblemListener listener, @NotNull Disposable parentDisposable) {
    myProject.getMessageBus().connect(parentDisposable).subscribe(com.intellij.problems.ProblemListener.TOPIC, listener);
  }

  @Override
  public void queue(@NotNull VirtualFile suspiciousFile) {
    if (!isToBeHighlighted(suspiciousFile)) return;
    doQueue(suspiciousFile);
  }

  private void doQueue(@NotNull VirtualFile suspiciousFile) {
    synchronized (myCheckingQueue) {
      myCheckingQueue.add(suspiciousFile);
    }
  }

  @Override
  public boolean isProblemFile(@NotNull VirtualFile virtualFile) {
    return hasRegularProblems(virtualFile) || hasProblemsFromExternalSources(virtualFile);
  }

  private boolean hasRegularProblems(@NotNull VirtualFile virtualFile) {
    synchronized (myProblems) {
      if (myProblems.containsKey(virtualFile)) return true;
    }
    return false;
  }

  private boolean hasProblemsFromExternalSources(@NotNull VirtualFile virtualFile) {
    synchronized (myProblemsFromExternalSources) {
      if (myProblemsFromExternalSources.containsKey(virtualFile)) return true;
    }
    return false;
  }

  private boolean isToBeHighlighted(@NotNull VirtualFile virtualFile) {
    for (Condition<VirtualFile> filter : FILTER_EP_NAME.getExtensions(myProject)) {
      ProgressManager.checkCanceled();
      if (filter.value(virtualFile)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void weHaveGotProblems(@NotNull VirtualFile virtualFile, @NotNull List<? extends Problem> problems) {
    if (problems.isEmpty()) return;
    if (!isToBeHighlighted(virtualFile)) return;
    weHaveGotNonIgnorableProblems(virtualFile, problems);
  }

  @Override
  public void weHaveGotNonIgnorableProblems(@NotNull VirtualFile virtualFile, @NotNull List<? extends Problem> problems) {
    if (problems.isEmpty()) return;
    boolean fireListener = false;
    synchronized (myProblems) {
      ProblemFileInfo storedProblems = myProblems.get(virtualFile);
      if (storedProblems == null) {
        storedProblems = new ProblemFileInfo();

        myProblems.put(virtualFile, storedProblems);
        fireListener = true;
      }
      storedProblems.problems.addAll(problems);
    }
    doQueue(virtualFile);
    if (fireListener) {
      if (hasProblemsFromExternalSources(virtualFile)) {
        fireProblemsChanged(virtualFile);
      }
      else {
        fireProblemsAppeared(virtualFile);
      }
    }
  }

  private void fireProblemsAppeared(@NotNull VirtualFile file) {
    myProject.getMessageBus().syncPublisher(com.intellij.problems.ProblemListener.TOPIC).problemsAppeared(file);
  }

  private void fireProblemsChanged(@NotNull VirtualFile virtualFile) {
    myProject.getMessageBus().syncPublisher(com.intellij.problems.ProblemListener.TOPIC).problemsChanged(virtualFile);
  }

  private void fireProblemsDisappeared(@NotNull VirtualFile problemFile) {
    myProject.getMessageBus().syncPublisher(com.intellij.problems.ProblemListener.TOPIC).problemsDisappeared(problemFile);
  }

  @Override
  public void clearProblems(@NotNull VirtualFile virtualFile) {
    doRemove(virtualFile);
  }

  @Override
  public Problem convertToProblem(@NotNull VirtualFile virtualFile,
                                  int line,
                                  int column,
                                  String @NotNull [] message) {
    if (virtualFile.isDirectory() || virtualFile.getFileType().isBinary()) return null;
    HighlightInfo info = ReadAction.compute(() -> {
      TextRange textRange = getTextRange(virtualFile, line, column);
      String description = StringUtil.join(message, "\n");
      return HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(textRange).descriptionAndTooltip(description).create();
    });
    if (info == null) return null;
    return new ProblemImpl(virtualFile, info, false);
  }

  @Override
  public void reportProblems(@NotNull VirtualFile file, @NotNull Collection<? extends Problem> problems) {
    if (problems.isEmpty()) {
      clearProblems(file);
      return;
    }
    if (!isToBeHighlighted(file)) return;
    boolean hasProblemsBefore;
    boolean fireChanged;
    synchronized (myProblems) {
      ProblemFileInfo oldInfo = myProblems.remove(file);
      hasProblemsBefore = oldInfo != null;
      ProblemFileInfo newInfo = new ProblemFileInfo();
      myProblems.put(file, newInfo);
      for (Problem problem : problems) {
        newInfo.problems.add(problem);
        newInfo.hasSyntaxErrors |= ((ProblemImpl)problem).isSyntaxOnly();
      }
      fireChanged = hasProblemsBefore && !oldInfo.equals(newInfo);
    }
    doQueue(file);
    boolean hasExternal = hasProblemsFromExternalSources(file);
    if (!hasProblemsBefore && !hasExternal) {
      fireProblemsAppeared(file);
    }
    else if (fireChanged || hasExternal) {
      fireProblemsChanged(file);
    }
  }

  @Override
  public void reportProblemsFromExternalSource(@NotNull VirtualFile file, @NotNull Object source) {
    if (!isToBeHighlighted(file)) return;

    boolean isNewFileForExternalSource;
    synchronized (myProblemsFromExternalSources) {
      if (myProblemsFromExternalSources.containsKey(file)) {
        isNewFileForExternalSource = false;
        myProblemsFromExternalSources.get(file).add(source);
      }
      else {
        myProblemsFromExternalSources.put(file, ContainerUtil.newHashSet(source));
        isNewFileForExternalSource = true;
      }
    }

    if (isNewFileForExternalSource && !hasRegularProblems(file)) {
      fireProblemsAppeared(file);
    }
    else {
      fireProblemsChanged(file);
    }
  }


  @Override
  public void clearProblemsFromExternalSource(@NotNull VirtualFile file, @NotNull Object source) {
    boolean isLastExternalSource = false;
    synchronized (myProblemsFromExternalSources) {
      Set<Object> sources = myProblemsFromExternalSources.get(file);
      if (sources == null) return;

      sources.remove(source);
      if (sources.isEmpty()) {
        isLastExternalSource = true;
        myProblemsFromExternalSources.remove(file);
      }
    }


    if (isLastExternalSource && !hasRegularProblems(file)) {
      fireProblemsDisappeared(file);
    }
    else {
      fireProblemsChanged(file);
    }
  }

  @NotNull
  private static TextRange getTextRange(@NotNull VirtualFile virtualFile, int line, int column) {
    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (line > document.getLineCount()) line = document.getLineCount();
    line = line <= 0 ? 0 : line - 1;
    int offset = document.getLineStartOffset(line) + (column <= 0 ? 0 : column - 1);
    return new TextRange(offset, offset);
  }

  public boolean processProblemFiles(@NotNull Processor<? super VirtualFile> processor) {
    List<VirtualFile> files;
    synchronized (myProblems) {
      files = new ArrayList<>(myProblems.keySet());
    }
    return ContainerUtil.process(files, processor);
  }
}
