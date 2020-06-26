// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.ProjectTopics;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.LineMarkerProviders;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSettingListener;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetManagerAdapter;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.ide.todo.TodoConfiguration;
import com.intellij.lang.ExternalLanguageAnnotators;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorActivityManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx;
import com.intellij.openapi.editor.ex.ErrorStripeEvent;
import com.intellij.openapi.editor.ex.ErrorStripeListener;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.profile.ProfileChangeAdapter;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.ThreeState;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DaemonListeners implements Disposable {
  private static final Logger LOG = Logger.getInstance(DaemonListeners.class);

  private final Project myProject;
  private final DaemonCodeAnalyzerImpl myDaemonCodeAnalyzer;

  private boolean myEscPressed;

  private volatile boolean cutOperationJustHappened;
  private final DaemonCodeAnalyzer.DaemonListener myDaemonEventPublisher;
  private List<Editor> myActiveEditors = Collections.emptyList();

  private static final Key<Boolean> DAEMON_INITIALIZED = Key.create("DAEMON_INITIALIZED");

  public static DaemonListeners getInstance(Project project) {
    return project.getComponent(DaemonListeners.class);
  }

  public DaemonListeners(@NotNull Project project) {
    myProject = project;
    myDaemonCodeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(myProject);

    boolean replaced = ((UserDataHolderEx)myProject).replace(DAEMON_INITIALIZED, null, Boolean.TRUE);
    if (!replaced) {
      LOG.error("Daemon listeners already initialized for the project " + myProject);
    }

    MessageBus messageBus = myProject.getMessageBus();
    myDaemonEventPublisher = messageBus.syncPublisher(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC);
    if (project.isDefault()) {
      return;
    }

    MessageBusConnection connection = messageBus.connect();
    connection.subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener() {
      @Override
      public void appClosing() {
        stopDaemon(false, "App closing");
      }
    });

    EditorFactory editorFactory = EditorFactory.getInstance();
    EditorEventMulticasterEx eventMulticaster = (EditorEventMulticasterEx)editorFactory.getEventMulticaster();
    eventMulticaster.addDocumentListener(new DocumentListener() {
      // clearing highlighters before changing document because change can damage editor highlighters drastically, so we'll clear more than necessary
      @Override
      public void beforeDocumentChange(@NotNull DocumentEvent e) {
        Document document = e.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        Project project = virtualFile == null ? null : ProjectUtil.guessProjectForFile(virtualFile);
        //no need to stop daemon if something happened in the console or in non-physical document
        if (!myProject.isDisposed() && ApplicationManager.getApplication().isDispatchThread() && worthBothering(document, project)) {
          stopDaemon(true, "Document change");
          UpdateHighlightersUtil.updateHighlightersByTyping(myProject, e);
        }
      }
    }, this);

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      eventMulticaster.addCaretListener(new CaretListener() {
        @Override
        public void caretPositionChanged(@NotNull CaretEvent e) {
          Editor editor = e.getEditor();
          if (EditorActivityManager.getInstance().isVisible(editor) &&
              worthBothering(editor.getDocument(), editor.getProject())) {
            ApplicationManager.getApplication().invokeLater(() -> {
              if (!myProject.isDisposed() && EditorActivityManager.getInstance().isVisible(editor)) {
                IntentionsUI.getInstance(myProject).invalidate();
              }
            }, ModalityState.current(), myProject.getDisposed());
          }
        }
      }, this);
    }

    connection.subscribe(EditorTrackerListener.TOPIC, new EditorTrackerListener() {
      @Override
      public void activeEditorsChanged(@NotNull List<Editor> activeEditors) {
        if (myActiveEditors.equals(activeEditors)) {
          return;
        }

        myActiveEditors = new ArrayList<>(activeEditors);
        // do not stop daemon if idea loses/gains focus
        DaemonListeners.this.stopDaemon(true, "Active editor change");
        if (ApplicationManager.getApplication().isDispatchThread() && LaterInvocator.isInModalContext()) {
          // editor appear in modal context, re-enable the daemon
          myDaemonCodeAnalyzer.setUpdateByTimerEnabled(true);
        }

        ErrorStripeUpdateManager errorStripeUpdateManager = ErrorStripeUpdateManager.getInstance(myProject);
        for (Editor editor : activeEditors) {
          errorStripeUpdateManager.repaintErrorStripePanel(editor);
        }
      }
    });

    editorFactory.addEditorFactoryListener(new EditorFactoryListener() {
      @Override
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        if (myProject.isDisposed()) {
          return;
        }

        Editor editor = event.getEditor();
        Document document = editor.getDocument();
        Project editorProject = editor.getProject();
        // worthBothering() checks for getCachedPsiFile, so call getPsiFile here
        PsiFile file = editorProject == null ? null : PsiDocumentManager.getInstance(editorProject).getPsiFile(document);
        boolean showing = EditorActivityManager.getInstance().isVisible(editor);
        boolean worthBothering = worthBothering(document, editorProject);
        if (!showing || !worthBothering) {
          LOG.debug("Not worth bothering about editor created for : " + file + " because editor isShowing(): " +
                    showing + "; project is open and file is mine: " + worthBothering);
          return;
        }

        ErrorStripeUpdateManager.getInstance(myProject).repaintErrorStripePanel(editor);
      }

      @Override
      public void editorReleased(@NotNull EditorFactoryEvent event) {
        myActiveEditors.remove(event.getEditor());
        // mem leak after closing last editor otherwise
        UIUtil.invokeLaterIfNeeded(() -> {
          IntentionsUI intentionUI = myProject.getServiceIfCreated(IntentionsUI.class);
          if (intentionUI != null) {
            intentionUI.invalidate();
          }
        });
      }
    }, this);

    PsiManager.getInstance(myProject).addPsiTreeChangeListener(new PsiChangeHandler(myProject, connection, this), this);

    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        stopDaemonAndRestartAllFiles("Project roots changed");
      }
    });

    connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @Override
      public void enteredDumbMode() {
        stopDaemonAndRestartAllFiles("Dumb mode started");
      }

      @Override
      public void exitDumbMode() {
        stopDaemonAndRestartAllFiles("Dumb mode finished");
      }
    });

    connection.subscribe(PowerSaveMode.TOPIC, () -> stopDaemon(true, "Power save mode change"));
    connection.subscribe(EditorColorsManager.TOPIC, __ -> stopDaemonAndRestartAllFiles("Editor color scheme changed"));
    connection.subscribe(CommandListener.TOPIC, new MyCommandListener());
    connection.subscribe(ProfileChangeAdapter.TOPIC, new MyProfileChangeListener());

    ApplicationManager.getApplication().addApplicationListener(new MyApplicationListener(), project);

    connection.subscribe(TodoConfiguration.PROPERTY_CHANGE, new MyTodoListener());

    connection.subscribe(AnActionListener.TOPIC, new MyAnActionListener());
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        boolean isDaemonShouldBeStopped = false;
        for (VFileEvent event : events) {
          if (event instanceof VFilePropertyChangeEvent) {
            VFilePropertyChangeEvent e = (VFilePropertyChangeEvent)event;
            String propertyName = e.getPropertyName();
            if (VirtualFile.PROP_NAME.equals(propertyName)) {
              fileRenamed(e);
            }
            if (!isDaemonShouldBeStopped && !propertyName.equals(PsiTreeChangeEvent.PROP_WRITABLE)) {
              isDaemonShouldBeStopped = true;
            }
          }
        }

        if (isDaemonShouldBeStopped) {
          stopDaemon(true, "Virtual file property change");
        }
      }

      private void fileRenamed(@NotNull VFilePropertyChangeEvent event) {
        stopDaemonAndRestartAllFiles("Virtual file name changed");
        VirtualFile virtualFile = event.getFile();
        PsiFile psiFile = !virtualFile.isValid() ? null : PsiManagerEx.getInstanceEx(myProject).getFileManager().getCachedPsiFile(virtualFile);
        if (psiFile == null || myDaemonCodeAnalyzer.isHighlightingAvailable(psiFile)) {
          return;
        }

        Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
        if (document == null) {
          return;
        }

        // highlight markers no more
        //todo clear all highlights regardless the pass id

        // Here color scheme required for TextEditorFields, as far as I understand this
        // code related to standard file editors, which always use Global color scheme,
        // thus we can pass null here.
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, document, 0, document.getTextLength(),
                                                       Collections.emptyList(),
                                                       null,
                                                       Pass.UPDATE_ALL);
      }
    });
    connection.subscribe(FileTypeManager.TOPIC, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@NotNull FileTypeEvent event) {
        IntentionsUI.getInstance(project).invalidate();
      }
    });

    eventMulticaster.addErrorStripeListener(new ErrorStripeListener() {
      @Override
      public void errorMarkerClicked(@NotNull ErrorStripeEvent e) {
        RangeHighlighter highlighter = e.getHighlighter();
        if (!highlighter.isValid()) return;
        HighlightInfo info = HighlightInfo.fromRangeHighlighter(highlighter);
        if (info != null) {
          GotoNextErrorHandler.navigateToError(myProject, e.getEditor(), info, null);
        }
      }
    }, this);

    LaterInvocator.addModalityStateListener(new ModalityStateListener() {
      @Override
      public void beforeModalityStateChanged(boolean entering, @NotNull Object modalEntity) {
        // before showing dialog we are in non-modal context yet, and before closing dialog we are still in modal context
        boolean inModalContext = Registry.is("ide.perProjectModality") || LaterInvocator.isInModalContext();
        stopDaemon(inModalContext, "Modality change. Was modal: " + inModalContext);
        myDaemonCodeAnalyzer.setUpdateByTimerEnabled(inModalContext);
      }
    }, this);

    connection.subscribe(SeverityRegistrar.SEVERITIES_CHANGED_TOPIC, () -> stopDaemonAndRestartAllFiles("Severities changed"));

    connection.subscribe(FacetManager.FACETS_TOPIC, new FacetManagerAdapter() {
      @Override
      public void facetRenamed(@NotNull Facet facet, @NotNull String oldName) {
        stopDaemonAndRestartAllFiles("facet renamed: " + oldName + " -> " + facet.getName());
      }

      @Override
      public void facetAdded(@NotNull Facet facet) {
        stopDaemonAndRestartAllFiles("facet added: " + facet.getName());
      }

      @Override
      public void facetRemoved(@NotNull Facet facet) {
        stopDaemonAndRestartAllFiles("facet removed: " + facet.getName());
      }

      @Override
      public void facetConfigurationChanged(@NotNull Facet facet) {
        stopDaemonAndRestartAllFiles("facet changed: " + facet.getName());
      }
    });

    restartOnExtensionChange(LanguageAnnotators.EP_NAME, "annotators list changed");
    restartOnExtensionChange(LineMarkerProviders.EP_NAME, "line marker providers list changed");
    restartOnExtensionChange(ExternalLanguageAnnotators.EP_NAME, "external annotators list changed");

    connection.subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
      @Override
      public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        ((PsiModificationTrackerImpl)PsiManager.getInstance(myProject).getModificationTracker()).incCounter();
        stopDaemonAndRestartAllFiles("Plugin installed");
      }

      @Override
      public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        ((PsiModificationTrackerImpl)PsiManager.getInstance(myProject).getModificationTracker()).incCounter();
        stopDaemon(false, "Plugin will be uninstalled");
        removeHighlightersOnPluginUnload(pluginDescriptor);
        myDaemonCodeAnalyzer.clearProgressIndicator();
        IntentionsUI.getInstance(project).invalidate();
      }

      @Override
      public void pluginUnloaded(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        stopDaemonAndRestartAllFiles("Plugin unloaded");
      }
    });
    connection.subscribe(FileHighlightingSettingListener.SETTING_CHANGE, (root, setting) ->
      WriteAction.run(() -> {
        PsiFile file = root.getContainingFile();
        if (file != null) {
          // force clearing all PSI caches, including those in WholeFileInspectionFactory
          ((PsiModificationTrackerImpl)PsiManager.getInstance(myProject).getModificationTracker()).incCounter();
        }
      }));
  }

  private <T, U extends KeyedLazyInstance<T>> void restartOnExtensionChange(@NotNull ExtensionPointName<U> name, @NotNull String message) {
    name.addChangeListener(() -> stopDaemonAndRestartAllFiles(message), this);
  }

  private boolean worthBothering(@Nullable Document document, Project project) {
    if (document == null) {
      return true;
    }
    if (project != null && project != myProject) {
      return false;
    }
    // cached is essential here since we do not want to create PSI file in alien project
    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(document);
    return psiFile != null && psiFile.isPhysical() && psiFile.getOriginalFile() == psiFile;
  }

  @Override
  public void dispose() {
    stopDaemonAndRestartAllFiles("Project closed");
    boolean replaced = ((UserDataHolderEx)myProject).replace(DAEMON_INITIALIZED, Boolean.TRUE, Boolean.FALSE);
    LOG.assertTrue(replaced, "Daemon listeners already disposed for the project "+myProject);
  }

  public static boolean canChangeFileSilently(@NotNull PsiFileSystemItem file) {
    Project project = file.getProject();
    DaemonListeners listeners = getInstance(project);
    if (listeners == null) return true;

    if (listeners.cutOperationJustHappened) return false;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return false;
    if (file instanceof PsiCodeFragment) return true;
    if (ScratchUtil.isScratch(virtualFile)) return listeners.canUndo(virtualFile);
    if (!ModuleUtilCore.projectContainsFile(project, virtualFile, false)) return false;

    for (SilentChangeVetoer extension : SilentChangeVetoer.EP_NAME.getExtensionList()) {
      ThreeState result = extension.canChangeFileSilently(project, virtualFile);
      if (result != ThreeState.UNSURE) return result.toBoolean();
    }

    return listeners.canUndo(virtualFile);
  }

  private boolean canUndo(@NotNull VirtualFile virtualFile) {
    FileEditor[] editors = FileEditorManager.getInstance(myProject).getEditors(virtualFile);
    if (editors.length == 0) {
      return false;
    }

    UndoManager undoManager = UndoManager.getInstance(myProject);
    for (FileEditor editor : editors) {
      if (undoManager.isUndoAvailable(editor)) {
        return true;
      }
    }
    return false;
  }

  private class MyApplicationListener implements ApplicationListener {
    @Override
    public void beforeWriteActionStart(@NotNull Object action) {
      if (!myDaemonCodeAnalyzer.isRunning()) return; // we'll restart in writeActionFinished()
      stopDaemon(true, "Write action start");
    }

    @Override
    public void writeActionFinished(@NotNull Object action) {
      stopDaemon(true, "Write action finish");
    }
  }

  private static final class Holder {
    private static final String myCutActionName = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_CUT).getTemplatePresentation().getText();
  }

  private final class MyCommandListener implements CommandListener {
    @Override
    public void commandStarted(@NotNull CommandEvent event) {
      Document affectedDocument = extractDocumentFromCommand(event);
      if (!worthBothering(affectedDocument, event.getProject())) {
        return;
      }

      cutOperationJustHappened = Comparing.strEqual(Holder.myCutActionName, event.getCommandName());
      if (!myDaemonCodeAnalyzer.isRunning()) return;
      if (LOG.isDebugEnabled()) {
        LOG.debug("cancelling code highlighting by command:" + event.getCommand());
      }
      stopDaemon(false, "Command start");
    }

    private @Nullable Document extractDocumentFromCommand(@NotNull CommandEvent event) {
      Document affectedDocument = event.getDocument();
      if (affectedDocument != null) return affectedDocument;
      Object id = event.getCommandGroupId();

      if (id instanceof Document) {
        affectedDocument = (Document)id;
      }
      else if (id instanceof DocCommandGroupId) {
        affectedDocument = ((DocCommandGroupId)id).getDocument();
      }
      return affectedDocument;
    }

    @Override
    public void commandFinished(@NotNull CommandEvent event) {
      Document affectedDocument = extractDocumentFromCommand(event);
      if (!worthBothering(affectedDocument, event.getProject())) {
        return;
      }

      if (myEscPressed) {
        myEscPressed = false;
        if (affectedDocument != null) {
          // prevent Esc key to leave the document in the not-highlighted state
          if (!myDaemonCodeAnalyzer.getFileStatusMap().allDirtyScopesAreNull(affectedDocument)) {
            stopDaemon(true, "Command finish");
          }
        }
      }
      else if (!myDaemonCodeAnalyzer.isRunning()) {
        stopDaemon(true, "Command finish");
      }
    }
  }

  private final class MyTodoListener implements PropertyChangeListener {
    @Override
    public void propertyChange(@NotNull PropertyChangeEvent evt) {
      if (TodoConfiguration.PROP_TODO_PATTERNS.equals(evt.getPropertyName())) {
        stopDaemonAndRestartAllFiles("Todo patterns changed");
      }
      else if (TodoConfiguration.PROP_MULTILINE.equals(evt.getPropertyName())) {
        stopDaemonAndRestartAllFiles("Todo multi-line detection changed");
      }
    }
  }

  private class MyProfileChangeListener implements ProfileChangeAdapter {
    @Override
    public void profileChanged(InspectionProfile profile) {
      stopDaemonAndRestartAllFiles("Profile changed");
    }

    @Override
    public void profileActivated(InspectionProfile oldProfile, @Nullable InspectionProfile profile) {
      stopDaemonAndRestartAllFiles("Profile activated");
    }

    @Override
    public void profilesInitialized() {
      AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> stopDaemonAndRestartAllFiles("Inspection profiles activated"));
    }
  }

  private final class MyAnActionListener implements AnActionListener {
    private AnAction cachedEscapeAction;

    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
      if (cachedEscapeAction == null) {
        myEscPressed = IdeActions.ACTION_EDITOR_ESCAPE.equals(event.getActionManager().getId(action));
        if (myEscPressed) {
          cachedEscapeAction = action;
        }
      }
      else {
        myEscPressed = cachedEscapeAction == action;
      }
    }

    @Override
    public void beforeEditorTyping(char c, @NotNull DataContext dataContext) {
      Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
      //no need to stop daemon if something happened in the console
      if (editor != null && !worthBothering(editor.getDocument(), editor.getProject())) {
        return;
      }
      stopDaemon(true, "Editor typing");
    }
  }

  private void stopDaemon(boolean toRestartAlarm, @NonNls @NotNull String reason) {
    if (myDaemonCodeAnalyzer.stopProcess(toRestartAlarm, reason)) {
      myDaemonEventPublisher.daemonCancelEventOccurred(reason);
    }
  }

  private void stopDaemonAndRestartAllFiles(@NotNull String reason) {
    if (myDaemonCodeAnalyzer.doRestart(reason) && !myProject.isDisposed()) {
      myDaemonEventPublisher.daemonCancelEventOccurred(reason);
    }
  }

  private void removeHighlightersOnPluginUnload(@NotNull PluginDescriptor pluginDescriptor) {
    for (FileEditor fileEditor : FileEditorManager.getInstance(myProject).getAllEditors()) {
      if (fileEditor instanceof TextEditor) {
        Editor editor = ((TextEditor)fileEditor).getEditor();
        removeHighlightersOnPluginUnload(editor.getMarkupModel(), pluginDescriptor);
        MarkupModel documentMarkupModel = DocumentMarkupModel.forDocument(editor.getDocument(), myProject, false);
        if (documentMarkupModel != null) {
          removeHighlightersOnPluginUnload(documentMarkupModel, pluginDescriptor);
        }
      }
    }
  }

  private static void removeHighlightersOnPluginUnload(@NotNull MarkupModel model, @NotNull PluginDescriptor pluginDescriptor) {
    for (RangeHighlighter highlighter: model.getAllHighlighters()) {
      if (!(highlighter instanceof RangeHighlighterEx && ((RangeHighlighterEx)highlighter).isPersistent())) {
        model.removeHighlighter(highlighter);
      }
      else if (pluginDescriptor.getPluginClassLoader() instanceof PluginClassLoader) {
        CustomHighlighterRenderer renderer = highlighter.getCustomRenderer();
        if (renderer != null && renderer.getClass().getClassLoader() == pluginDescriptor.getPluginClassLoader()) {
          model.removeHighlighter(highlighter);
        }
      }
    }
  }
}
