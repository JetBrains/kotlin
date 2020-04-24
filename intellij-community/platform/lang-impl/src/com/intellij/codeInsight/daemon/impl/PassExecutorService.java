// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.*;
import com.intellij.concurrency.Job;
import com.intellij.concurrency.JobLauncher;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorActivityManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Functions;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PassExecutorService implements Disposable {
  static final Logger LOG = Logger.getInstance(PassExecutorService.class);
  private static final boolean CHECK_CONSISTENCY = ApplicationManager.getApplication().isUnitTestMode();

  private final Map<ScheduledPass, Job<Void>> mySubmittedPasses = new ConcurrentHashMap<>();
  private final Project myProject;
  private volatile boolean isDisposed;
  private final AtomicInteger nextAvailablePassId; // used to assign random id to a pass if not set

  PassExecutorService(@NotNull Project project) {
    myProject = project;
    nextAvailablePassId = ((TextEditorHighlightingPassRegistrarImpl)TextEditorHighlightingPassRegistrar.getInstance(myProject)).getNextAvailableId();
  }

  @Override
  public void dispose() {
    cancelAll(true);
    // some workers could, although idle, still retain some thread references for some time causing leak hunter to frown
    ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.SECONDS);
    isDisposed = true;
  }

  void cancelAll(boolean waitForTermination) {
    for (Job<Void> submittedPass : mySubmittedPasses.values()) {
      submittedPass.cancel();
    }
    try {
      if (waitForTermination) {
        while (!waitFor(50)) {
          int i = 0;
        }
      }
    }
    catch (ProcessCanceledException ignored) {
    }
    catch (Error | RuntimeException e) {
      throw e;
    }
    catch (Throwable throwable) {
      LOG.error(throwable);
    }
    finally {
      mySubmittedPasses.clear();
    }
  }

  void submitPasses(@NotNull Map<FileEditor, HighlightingPass[]> passesMap, @NotNull DaemonProgressIndicator updateProgress) {
    if (isDisposed()) return;

    // null keys are ok
    MultiMap<Document, FileEditor> documentToEditors = MultiMap.createSet();
    MultiMap<FileEditor, TextEditorHighlightingPass> documentBoundPasses = MultiMap.createSmart();
    MultiMap<FileEditor, EditorBoundHighlightingPass> editorBoundPasses = MultiMap.createSmart();
    List<Pair<FileEditor, TextEditorHighlightingPass>> passesWithNoDocuments = new ArrayList<>();
    Map<FileEditor, TIntObjectHashMap<TextEditorHighlightingPass>> id2Pass = new THashMap<>();
    Set<VirtualFile> vFiles = new HashSet<>();

    for (Map.Entry<FileEditor, HighlightingPass[]> entry : passesMap.entrySet()) {
      FileEditor fileEditor = entry.getKey();
      HighlightingPass[] passes = entry.getValue();
      Document document;
      if (fileEditor instanceof TextEditor) {
        Editor editor = ((TextEditor)fileEditor).getEditor();
        LOG.assertTrue(!(editor instanceof EditorWindow));
        document = editor.getDocument();
      }
      else {
        VirtualFile virtualFile = ((FileEditorManagerEx)FileEditorManager.getInstance(myProject)).getFile(fileEditor);
        document = virtualFile == null ? null : FileDocumentManager.getInstance().getDocument(virtualFile);
      }
      if (document != null) {
        vFiles.add(FileDocumentManager.getInstance().getFile(document));
      }

      int prevId = 0;
      for (final HighlightingPass pass : passes) {
        TIntObjectHashMap<TextEditorHighlightingPass> thisEditorId2Pass = id2Pass.computeIfAbsent(fileEditor, __ -> new TIntObjectHashMap<>(20));
        if (pass instanceof EditorBoundHighlightingPass) {
          EditorBoundHighlightingPass editorPass = (EditorBoundHighlightingPass)pass;
          int id = nextAvailablePassId.incrementAndGet();
          editorPass.setId(id); // have to make ids unique for this document
          checkUniquePassId(id, editorPass, thisEditorId2Pass);
          editorBoundPasses.putValue(fileEditor, editorPass);
        }
        else {
          TextEditorHighlightingPass convertedPass;
          if (pass instanceof TextEditorHighlightingPass) {
            convertedPass = (TextEditorHighlightingPass)pass;
          }
          else {
            // run all passes in sequence
            convertedPass = convertToTextHighlightingPass(pass, document, prevId);
            convertedPass.setId(nextAvailablePassId.incrementAndGet());
          }
          checkUniquePassId(convertedPass.getId(), convertedPass, thisEditorId2Pass);
          document = convertedPass.getDocument();
          documentBoundPasses.putValue(fileEditor, convertedPass);
          if (document == null) {
            passesWithNoDocuments.add(Pair.create(fileEditor, convertedPass));
          }
          else {
            documentToEditors.putValue(document, fileEditor);
          }
          prevId = convertedPass.getId();
        }
      }
    }

    List<ScheduledPass> freePasses = new ArrayList<>(documentToEditors.size() * 5);
    List<ScheduledPass> dependentPasses = new ArrayList<>(documentToEditors.size() * 10);
    // fileEditor-> (passId -> created pass)
    Map<FileEditor, TIntObjectHashMap<ScheduledPass>> toBeSubmitted = new THashMap<>(passesMap.size());

    final AtomicInteger threadsToStartCountdown = new AtomicInteger(0);
    for (Map.Entry<Document, Collection<FileEditor>> entry : documentToEditors.entrySet()) {
      Collection<FileEditor> fileEditors = entry.getValue();
      Document document = entry.getKey();
      FileEditor preferredFileEditor = getPreferredFileEditor(document, fileEditors);
      List<TextEditorHighlightingPass> passes = (List<TextEditorHighlightingPass>)documentBoundPasses.get(preferredFileEditor);
      if (passes.isEmpty()) {
        continue;
      }
      sortById(passes);
      for (TextEditorHighlightingPass currentPass : passes) {
        createScheduledPass(preferredFileEditor, currentPass, toBeSubmitted, id2Pass, freePasses, dependentPasses, updateProgress, threadsToStartCountdown);
      }
    }

    for (Map.Entry<FileEditor, Collection<EditorBoundHighlightingPass>> entry : editorBoundPasses.entrySet()) {
      FileEditor fileEditor = entry.getKey();
      Collection<EditorBoundHighlightingPass> createdEditorBoundPasses = entry.getValue();
      for (EditorBoundHighlightingPass pass : createdEditorBoundPasses) {
        createScheduledPass(fileEditor, pass, toBeSubmitted, id2Pass, freePasses, dependentPasses, updateProgress, threadsToStartCountdown);
      }
    }

    for (Pair<FileEditor, TextEditorHighlightingPass> pair : passesWithNoDocuments) {
      FileEditor fileEditor = pair.first;
      TextEditorHighlightingPass pass = pair.second;
      createScheduledPass(fileEditor, pass, toBeSubmitted, id2Pass, freePasses, dependentPasses, updateProgress, threadsToStartCountdown);
    }

    if (CHECK_CONSISTENCY && !ApplicationInfoImpl.isInStressTest()) {
      assertConsistency(freePasses, toBeSubmitted, threadsToStartCountdown);
    }

    log(updateProgress, null, vFiles + " ----- starting " + threadsToStartCountdown.get(), freePasses);

    for (ScheduledPass dependentPass : dependentPasses) {
      mySubmittedPasses.put(dependentPass, Job.nullJob());
    }
    for (ScheduledPass freePass : freePasses) {
      submit(freePass);
    }
  }

  private static void checkUniquePassId(int id,
                                        @NotNull TextEditorHighlightingPass pass,
                                        @NotNull TIntObjectHashMap<TextEditorHighlightingPass> id2Pass) {
    TextEditorHighlightingPass prevPass = id2Pass.put(id, pass);
    if (prevPass != null) {
      LOG.error("Duplicate pass id found: "+id+". Both passes returned the same getId(): "+prevPass+" ("+prevPass.getClass() +") and "+pass+" ("+pass.getClass()+")");
    }
  }

  private void assertConsistency(@NotNull List<? extends ScheduledPass> freePasses,
                                 @NotNull Map<FileEditor, TIntObjectHashMap<ScheduledPass>> toBeSubmitted,
                                 @NotNull AtomicInteger threadsToStartCountdown) {
    assert threadsToStartCountdown.get() == toBeSubmitted.values().stream().mapToInt(m->m.size()).sum();
    TIntObjectHashMap<Pair<ScheduledPass, Integer>> id2Visits = new TIntObjectHashMap<>();
    for (ScheduledPass freePass : freePasses) {
      id2Visits.put(freePass.myPass.getId(), Pair.create(freePass, 0));
      checkConsistency(freePass, id2Visits);
    }
    id2Visits.forEachEntry((id, pair) -> {
      int count = pair.second;
      assert count == 0 : id;
      return true;
    });
    assert id2Visits.size() == threadsToStartCountdown.get();
  }

  private void checkConsistency(@NotNull ScheduledPass pass, @NotNull TIntObjectHashMap<Pair<ScheduledPass, Integer>> id2Visits) {
    for (ScheduledPass succ : ContainerUtil.concat(pass.mySuccessorsOnCompletion, pass.mySuccessorsOnSubmit)) {
      int succId = succ.myPass.getId();
      Pair<ScheduledPass, Integer> succPair = id2Visits.get(succId);
      if (succPair == null) {
        succPair = Pair.create(succ, succ.myRunningPredecessorsCount.get());
        id2Visits.put(succId, succPair);
      }
      int newPred = succPair.second - 1;
      id2Visits.put(succId, Pair.create(succ, newPred));
      assert newPred >= 0;
      if (newPred == 0) {
        checkConsistency(succ, id2Visits);
      }
    }
  }

  @NotNull
  private TextEditorHighlightingPass convertToTextHighlightingPass(@NotNull HighlightingPass pass,
                                                                   @Nullable Document document,
                                                                   int previousPassId) {
    TextEditorHighlightingPass textEditorHighlightingPass;
    textEditorHighlightingPass = new TextEditorHighlightingPass(myProject, document, true) {
      @Override
      public void doCollectInformation(@NotNull ProgressIndicator progress) {
        pass.collectInformation(progress);
      }

      @Override
      public void doApplyInformationToEditor() {
        pass.applyInformationToEditor();
        if (document != null) {
          VirtualFile file = FileDocumentManager.getInstance().getFile(document);
          FileEditor[] editors = file == null ? FileEditor.EMPTY_ARRAY : FileEditorManager.getInstance(myProject).getEditors(file);
          for (FileEditor editor : editors) {
            repaintErrorStripeAndIcon(editor);
          }
        }
      }
    };
    if (previousPassId != 0) {
      textEditorHighlightingPass.setCompletionPredecessorIds(new int[]{previousPassId});
    }
    return textEditorHighlightingPass;
  }

  @NotNull
  private FileEditor getPreferredFileEditor(Document document, @NotNull Collection<? extends FileEditor> fileEditors) {
    assert !fileEditors.isEmpty();
    if (document != null) {
      FileEditor focusedEditor = ContainerUtil.find(fileEditors, it -> it instanceof TextEditor &&
                                                                       ((TextEditor)it).getEditor().getContentComponent().isFocusOwner());
      if (focusedEditor != null) return focusedEditor;

      final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
      if (file != null) {
        final FileEditor selected = FileEditorManager.getInstance(myProject).getSelectedEditor(file);
        if (selected != null && fileEditors.contains(selected)) {
          return selected;
        }
      }
    }
    return fileEditors.iterator().next();
  }

  @NotNull
  private ScheduledPass createScheduledPass(@NotNull FileEditor fileEditor,
                                            @NotNull TextEditorHighlightingPass pass,
                                            @NotNull Map<FileEditor, TIntObjectHashMap<ScheduledPass>> toBeSubmitted,
                                            @NotNull Map<FileEditor, TIntObjectHashMap<TextEditorHighlightingPass>> id2Pass,
                                            @NotNull List<ScheduledPass> freePasses,
                                            @NotNull List<ScheduledPass> dependentPasses,
                                            @NotNull DaemonProgressIndicator updateProgress,
                                            @NotNull AtomicInteger threadsToStartCountdown) {
    TIntObjectHashMap<ScheduledPass> thisEditorId2ScheduledPass = toBeSubmitted.computeIfAbsent(fileEditor, __ -> new TIntObjectHashMap<>(20));
    TIntObjectHashMap<TextEditorHighlightingPass> thisEditorId2Pass = id2Pass.computeIfAbsent(fileEditor, __ -> new TIntObjectHashMap<>(20));
    int passId = pass.getId();
    ScheduledPass scheduledPass = thisEditorId2ScheduledPass.get(passId);
    if (scheduledPass != null) return scheduledPass;
    scheduledPass = new ScheduledPass(fileEditor, pass, updateProgress, threadsToStartCountdown);
    threadsToStartCountdown.incrementAndGet();
    thisEditorId2ScheduledPass.put(passId, scheduledPass);
    for (int predecessorId : pass.getCompletionPredecessorIds()) {
      ScheduledPass predecessor = findOrCreatePredecessorPass(fileEditor, toBeSubmitted, id2Pass, freePasses, dependentPasses,
                                                              updateProgress, threadsToStartCountdown, predecessorId,
                                                              thisEditorId2ScheduledPass, thisEditorId2Pass);
      if (predecessor != null) {
        predecessor.addSuccessorOnCompletion(scheduledPass);
      }
    }
    for (int predecessorId : pass.getStartingPredecessorIds()) {
      ScheduledPass predecessor = findOrCreatePredecessorPass(fileEditor, toBeSubmitted, id2Pass, freePasses, dependentPasses,
                                                              updateProgress, threadsToStartCountdown, predecessorId,
                                                              thisEditorId2ScheduledPass, thisEditorId2Pass);
      if (predecessor != null) {
        predecessor.addSuccessorOnSubmit(scheduledPass);
      }
    }
    if (scheduledPass.myRunningPredecessorsCount.get() == 0 && !freePasses.contains(scheduledPass)) {
      freePasses.add(scheduledPass);
    }
    else if (!dependentPasses.contains(scheduledPass)) {
      dependentPasses.add(scheduledPass);
    }

    if (pass.isRunIntentionPassAfter() && fileEditor instanceof TextEditor) {
      Editor editor = ((TextEditor)fileEditor).getEditor();
      ShowIntentionsPass ip = new ShowIntentionsPass(myProject, editor, false);
      int id = nextAvailablePassId.incrementAndGet();
      ip.setId(id);
      checkUniquePassId(id, ip, thisEditorId2Pass);
      ip.setCompletionPredecessorIds(new int[]{scheduledPass.myPass.getId()});

      createScheduledPass(fileEditor, ip, toBeSubmitted, id2Pass, freePasses, dependentPasses, updateProgress, threadsToStartCountdown);
    }

    return scheduledPass;
  }

  private ScheduledPass findOrCreatePredecessorPass(@NotNull FileEditor fileEditor,
                                                    @NotNull Map<FileEditor, TIntObjectHashMap<ScheduledPass>> toBeSubmitted,
                                                    @NotNull Map<FileEditor, TIntObjectHashMap<TextEditorHighlightingPass>> id2Pass,
                                                    @NotNull List<ScheduledPass> freePasses,
                                                    @NotNull List<ScheduledPass> dependentPasses,
                                                    @NotNull DaemonProgressIndicator updateProgress,
                                                    @NotNull AtomicInteger myThreadsToStartCountdown,
                                                    final int predecessorId,
                                                    @NotNull TIntObjectHashMap<ScheduledPass> thisEditorId2ScheduledPass,
                                                    @NotNull TIntObjectHashMap<TextEditorHighlightingPass> thisEditorId2Pass) {
    ScheduledPass predecessor = thisEditorId2ScheduledPass.get(predecessorId);
    if (predecessor == null) {
      TextEditorHighlightingPass textEditorPass = thisEditorId2Pass.get(predecessorId);
      predecessor = textEditorPass == null ? null : createScheduledPass(fileEditor, textEditorPass, toBeSubmitted,
                                                                        id2Pass, freePasses,
                                                                        dependentPasses, updateProgress, myThreadsToStartCountdown);
    }
    return predecessor;
  }

  private void submit(@NotNull ScheduledPass pass) {
    if (!pass.myUpdateProgress.isCanceled()) {
      Job<Void> job = JobLauncher.getInstance().submitToJobThread(pass, future -> {
        try {
          if (!future.isCancelled()) { // for canceled task .get() generates CancellationException which is expensive
            future.get();
          }
        }
        catch (CancellationException | InterruptedException ignored) {
        }
        catch (ExecutionException e) {
          LOG.error(e.getCause());
        }
      });
      mySubmittedPasses.put(pass, job);
    }
  }

  private class ScheduledPass implements Runnable {
    private final FileEditor myFileEditor;
    private final TextEditorHighlightingPass myPass;
    private final AtomicInteger myThreadsToStartCountdown;
    private final AtomicInteger myRunningPredecessorsCount = new AtomicInteger(0);
    private final List<ScheduledPass> mySuccessorsOnCompletion = new ArrayList<>();
    private final List<ScheduledPass> mySuccessorsOnSubmit = new ArrayList<>();
    @NotNull private final DaemonProgressIndicator myUpdateProgress;

    private ScheduledPass(@NotNull FileEditor fileEditor,
                          @NotNull TextEditorHighlightingPass pass,
                          @NotNull DaemonProgressIndicator progressIndicator,
                          @NotNull AtomicInteger threadsToStartCountdown) {
      myFileEditor = fileEditor;
      myPass = pass;
      myThreadsToStartCountdown = threadsToStartCountdown;
      myUpdateProgress = progressIndicator;
    }

    @Override
    public void run() {
      ((ApplicationImpl)ApplicationManager.getApplication()).executeByImpatientReader(() -> {
        try {
          doRun();
        }
        catch (ApplicationUtil.CannotRunReadActionException e) {
          myUpdateProgress.cancel();
        }
        catch (RuntimeException | Error e) {
          saveException(e, myUpdateProgress);
          throw e;
        }
      });
    }

    private void doRun() {
      if (myUpdateProgress.isCanceled()) return;

      log(myUpdateProgress, myPass, "Started. ");

      for (ScheduledPass successor : mySuccessorsOnSubmit) {
        int predecessorsToRun = successor.myRunningPredecessorsCount.decrementAndGet();
        if (predecessorsToRun == 0) {
          submit(successor);
        }
      }

      ProgressManager.getInstance().executeProcessUnderProgress(() -> {
        boolean success = ApplicationManagerEx.getApplicationEx().tryRunReadAction(() -> {
          try {
            if (DumbService.getInstance(myProject).isDumb() && !DumbService.isDumbAware(myPass)) {
              return;
            }

            if (!myUpdateProgress.isCanceled() && !myProject.isDisposed()) {
              myPass.collectInformation(myUpdateProgress);
            }
          }
          catch (ProcessCanceledException e) {
            log(myUpdateProgress, myPass, "Canceled ");

            if (!myUpdateProgress.isCanceled()) {
              myUpdateProgress.cancel(e); //in case when some smart asses throw PCE just for fun
            }
          }
          catch (RuntimeException | Error e) {
            myUpdateProgress.cancel(e);
            LOG.error(e);
            throw e;
          }
        });

        if (!success) {
          myUpdateProgress.cancel();
        }
      }, myUpdateProgress);

      log(myUpdateProgress, myPass, "Finished. ");

      if (!myUpdateProgress.isCanceled()) {
        applyInformationToEditorsLater(myFileEditor, myPass, myUpdateProgress, myThreadsToStartCountdown, ()->{
          for (ScheduledPass successor : mySuccessorsOnCompletion) {
            int predecessorsToRun = successor.myRunningPredecessorsCount.decrementAndGet();
            if (predecessorsToRun == 0) {
              submit(successor);
            }
          }
        });
      }
    }

    @NonNls
    @Override
    public String toString() {
      return "SP: " + myPass;
    }

    private void addSuccessorOnCompletion(@NotNull ScheduledPass successor) {
      mySuccessorsOnCompletion.add(successor);
      successor.myRunningPredecessorsCount.incrementAndGet();
    }

    private void addSuccessorOnSubmit(@NotNull ScheduledPass successor) {
      mySuccessorsOnSubmit.add(successor);
      successor.myRunningPredecessorsCount.incrementAndGet();
    }
  }

  private void applyInformationToEditorsLater(@NotNull final FileEditor fileEditor,
                                              @NotNull final TextEditorHighlightingPass pass,
                                              @NotNull final DaemonProgressIndicator updateProgress,
                                              @NotNull final AtomicInteger threadsToStartCountdown,
                                              @NotNull Runnable callbackOnApplied) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (isDisposed() || !fileEditor.isValid()) {
        updateProgress.cancel();
      }
      if (updateProgress.isCanceled()) {
        log(updateProgress, pass, " is canceled during apply, sorry");
        return;
      }
      Document document = pass.getDocument();
      try {
        if (fileEditor instanceof TextEditor && EditorActivityManager.getInstance().isVisible(((TextEditor)fileEditor).getEditor())
          || fileEditor.getComponent().isDisplayable()) {
          pass.applyInformationToEditor();
          repaintErrorStripeAndIcon(fileEditor);
          FileStatusMap fileStatusMap = DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap();
          if (document != null) {
            fileStatusMap.markFileUpToDate(document, pass.getId());
          }
          log(updateProgress, pass, " Applied");
        }
      }
      catch (ProcessCanceledException e) {
        log(updateProgress, pass, "Error " + e);
        throw e;
      }
      catch (RuntimeException e) {
        VirtualFile file = document == null ? null : FileDocumentManager.getInstance().getFile(document);
        FileType fileType = file == null ? null : file.getFileType();
        String message = "Exception while applying information to " + fileEditor + "("+fileType+")";
        log(updateProgress, pass, message + e);
        throw new RuntimeException(message, e);
      }
      if (threadsToStartCountdown.decrementAndGet() == 0) {
        HighlightingSessionImpl.waitForAllSessionsHighlightInfosApplied(updateProgress);
        log(updateProgress, pass, "Stopping ");
        updateProgress.stopIfRunning();
        clearStaleEntries();
      }
      else {
        log(updateProgress, pass, "Finished but there are passes in the queue: " + threadsToStartCountdown.get());
      }
      callbackOnApplied.run();
    }, updateProgress.getModalityState());
  }

  private void clearStaleEntries() {
    mySubmittedPasses.keySet().removeIf(pass -> pass.myUpdateProgress.isCanceled());
  }

  private void repaintErrorStripeAndIcon(@NotNull FileEditor fileEditor) {
    if (fileEditor instanceof TextEditor) {
      DefaultHighlightInfoProcessor.repaintErrorStripeAndIcon(((TextEditor)fileEditor).getEditor(), myProject);
    }
  }

  private boolean isDisposed() {
    return isDisposed || myProject.isDisposed();
  }

  @NotNull
  List<TextEditorHighlightingPass> getAllSubmittedPasses() {
    List<TextEditorHighlightingPass> result = new ArrayList<>(mySubmittedPasses.size());
    for (ScheduledPass scheduledPass : mySubmittedPasses.keySet()) {
      if (!scheduledPass.myUpdateProgress.isCanceled()) {
        result.add(scheduledPass.myPass);
      }
    }
    sortById(result);
    return result;
  }

  private static void sortById(@NotNull List<? extends TextEditorHighlightingPass> result) {
    ContainerUtil.quickSort(result, Comparator.comparingInt(TextEditorHighlightingPass::getId));
  }

  private static int getThreadNum() {
    Matcher matcher = Pattern.compile("JobScheduler FJ pool (\\d*)/(\\d*)").matcher(Thread.currentThread().getName());
    String num = matcher.matches() ? matcher.group(1) : null;
    return StringUtil.parseInt(num, 0);
  }

  static void log(ProgressIndicator progressIndicator, TextEditorHighlightingPass pass, @NonNls Object @NotNull ... info) {
    if (LOG.isDebugEnabled()) {
      CharSequence docText = pass == null || pass.getDocument() == null ? "" : ": '" + StringUtil.first(pass.getDocument().getCharsSequence(), 10, true)+ "'";
      synchronized (PassExecutorService.class) {
        String infos = StringUtil.join(info, Functions.TO_STRING(), " ");
        String message = StringUtil.repeatSymbol(' ', getThreadNum() * 4)
                         + " " + pass + " "
                         + infos
                         + "; progress=" + (progressIndicator == null ? null : progressIndicator.hashCode())
                         + " " + (progressIndicator == null ? "?" : progressIndicator.isCanceled() ? "X" : "V")
                         + docText;
        LOG.debug(message);
        //System.out.println(message);
      }
    }
  }

  private static final Key<Throwable> THROWABLE_KEY = Key.create("THROWABLE_KEY");
  private static void saveException(@NotNull Throwable e, @NotNull DaemonProgressIndicator indicator) {
    indicator.putUserDataIfAbsent(THROWABLE_KEY, e);
  }
  @TestOnly
  static Throwable getSavedException(@NotNull DaemonProgressIndicator indicator) {
    return indicator.getUserData(THROWABLE_KEY);
  }

  // return true if terminated
  boolean waitFor(int millis) throws Throwable {
    try {
      for (Job<Void> job : mySubmittedPasses.values()) {
        job.waitForCompletion(millis);
      }
      return true;
    }
    catch (TimeoutException ignored) {
      return false;
    }
    catch (InterruptedException e) {
      return true;
    }
    catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
