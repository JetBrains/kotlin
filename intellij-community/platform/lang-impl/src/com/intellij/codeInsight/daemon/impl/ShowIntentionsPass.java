// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInsight.intention.impl.CachedIntentions;
import com.intellij.codeInsight.intention.impl.EditIntentionSettingsAction;
import com.intellij.codeInsight.intention.impl.EnableDisableIntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.codeInsight.intention.impl.preview.IntentionPreviewUnsupportedOperationException;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorActivityManager;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Segment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ShowIntentionsPass extends TextEditorHighlightingPass {
  private final Editor myEditor;

  private final PsiFile myFile;
  private final int myPassIdToShowIntentionsFor;
  private final IntentionsInfo myIntentionsInfo = new IntentionsInfo();
  private final boolean myQueryIntentionActions;
  private volatile CachedIntentions myCachedIntentions;
  private volatile boolean myActionsChanged;

  /**
   *
   * @param queryIntentionActions true if {@link IntentionManager} must be asked for all registered {@link IntentionAction} and {@link IntentionAction#isAvailable(Project, Editor, PsiFile)} must be called on each
   *                              Usually, this expensive process should be executed only once per highlighting session
   */
  ShowIntentionsPass(@NotNull Project project, @NotNull Editor editor, boolean queryIntentionActions) {
    super(project, editor.getDocument(), false);
    myQueryIntentionActions = queryIntentionActions;
    myPassIdToShowIntentionsFor = -1;
    ApplicationManager.getApplication().assertIsDispatchThread();

    myEditor = editor;

    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);

    myFile = documentManager.getPsiFile(myEditor.getDocument());
    assert myFile != null : FileDocumentManager.getInstance().getFile(myEditor.getDocument());
  }

  public static @NotNull List<HighlightInfo.IntentionActionDescriptor> getAvailableFixes(final @NotNull Editor editor,
                                                                                         final @NotNull PsiFile file,
                                                                                         final int passId,
                                                                                         int offset) {
    final Project project = file.getProject();

    List<HighlightInfo.IntentionActionDescriptor> result = new ArrayList<>();
    DaemonCodeAnalyzerImpl.processHighlightsNearOffset(editor.getDocument(), project, HighlightSeverity.INFORMATION, offset, true,
                                                       info-> {
                                                         addAvailableFixesForGroups(info, editor, file, result, passId, offset);
                                                         return true;
                                                       });
    return result;
  }

  public static boolean markActionInvoked(@NotNull Project project,
                                          final @NotNull Editor editor,
                                          @NotNull IntentionAction action) {
    final int offset = ((EditorEx)editor).getExpectedCaretOffset();

    List<HighlightInfo> infos = new ArrayList<>();
    DaemonCodeAnalyzerImpl.processHighlightsNearOffset(editor.getDocument(), project, HighlightSeverity.INFORMATION, offset, true,
                                                       new CommonProcessors.CollectProcessor<>(infos));
    boolean removed = false;
    for (HighlightInfo info : infos) {
      if (info.quickFixActionMarkers != null) {
        for (Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker> pair : info.quickFixActionMarkers) {
          HighlightInfo.IntentionActionDescriptor actionInGroup = pair.first;
          if (actionInGroup.getAction() == action) {
            // no CME because the list is concurrent
            removed |= info.quickFixActionMarkers.remove(pair);
          }
        }
      }
    }
    return removed;
  }

  private static void addAvailableFixesForGroups(@NotNull HighlightInfo info,
                                                 @NotNull Editor editor,
                                                 @NotNull PsiFile file,
                                                 @NotNull List<? super HighlightInfo.IntentionActionDescriptor> outList,
                                                 int group,
                                                 int offset) {
    if (info.quickFixActionMarkers == null) return;
    if (group != -1 && group != info.getGroup()) return;
    boolean fixRangeIsNotEmpty = !info.getFixTextRange().isEmpty();
    Editor injectedEditor = null;
    PsiFile injectedFile = null;
    ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    for (Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker> pair : info.quickFixActionMarkers) {
      HighlightInfo.IntentionActionDescriptor actionInGroup = pair.first;
      RangeMarker range = pair.second;
      if (!range.isValid() || fixRangeIsNotEmpty && isEmpty(range)) continue;

      if (DumbService.isDumb(file.getProject()) && !DumbService.isDumbAware(actionInGroup.getAction())) {
        continue;
      }

      int start = range.getStartOffset();
      int end = range.getEndOffset();
      final Project project = file.getProject();
      if (start > offset || offset > end) {
        continue;
      }
      Editor editorToUse;
      PsiFile fileToUse;
      if (info.isFromInjection()) {
        if (injectedEditor == null) {
          injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(file, offset);
          injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, injectedFile);
        }
        editorToUse = injectedFile == null ? editor : injectedEditor;
        fileToUse = injectedFile == null ? file : injectedFile;
      }
      else {
        editorToUse = editor;
        fileToUse = file;
      }
      if (indicator != null) {
        indicator.setText(actionInGroup.getDisplayName());
      }
      if (actionInGroup.getAction().isAvailable(project, editorToUse, fileToUse)) {
        outList.add(actionInGroup);
      }
    }
  }

  private static boolean isEmpty(@NotNull Segment segment) {
    return segment.getEndOffset() <= segment.getStartOffset();
  }

  public static class IntentionsInfo {
    public final List<HighlightInfo.IntentionActionDescriptor> intentionsToShow = ContainerUtil.createLockFreeCopyOnWriteList();
    public final List<HighlightInfo.IntentionActionDescriptor> errorFixesToShow = ContainerUtil.createLockFreeCopyOnWriteList();
    public final List<HighlightInfo.IntentionActionDescriptor> inspectionFixesToShow = ContainerUtil.createLockFreeCopyOnWriteList();
    public final List<HighlightInfo.IntentionActionDescriptor> guttersToShow = ContainerUtil.createLockFreeCopyOnWriteList();
    public final List<HighlightInfo.IntentionActionDescriptor> notificationActionsToShow = ContainerUtil.createLockFreeCopyOnWriteList();
    private int myOffset;

    public void filterActions(@Nullable PsiFile psiFile) {
      IntentionActionFilter[] filters = IntentionActionFilter.EXTENSION_POINT_NAME.getExtensions();
      filter(intentionsToShow, psiFile, filters);
      filter(errorFixesToShow, psiFile, filters);
      filter(inspectionFixesToShow, psiFile, filters);
      filter(guttersToShow, psiFile, filters);
      filter(notificationActionsToShow, psiFile, filters);
    }

    public void setOffset(int offset) {
      myOffset = offset;
    }

    public int getOffset() {
      return myOffset;
    }

    private static void filter(@NotNull List<HighlightInfo.IntentionActionDescriptor> descriptors,
                               @Nullable PsiFile psiFile,
                               IntentionActionFilter @NotNull [] filters) {
      for (Iterator<HighlightInfo.IntentionActionDescriptor> it = descriptors.iterator(); it.hasNext(); ) {
        HighlightInfo.IntentionActionDescriptor actionDescriptor = it.next();
        for (IntentionActionFilter filter : filters) {
          if (!filter.accept(actionDescriptor.getAction(), psiFile)) {
            it.remove();
            break;
          }
        }
      }
    }

    public boolean isEmpty() {
      return intentionsToShow.isEmpty() && errorFixesToShow.isEmpty() && inspectionFixesToShow.isEmpty() && guttersToShow.isEmpty() &&
             notificationActionsToShow.isEmpty();
    }

    @NonNls
    @Override
    public String toString() {
      return
        "Errors: " + errorFixesToShow + "; " +
        "Inspection fixes: " + inspectionFixesToShow + "; " +
        "Intentions: " + intentionsToShow + "; " +
        "Gutters: " + guttersToShow + "; "+
        "Notifications: " + notificationActionsToShow;
    }
  }

  @Override
  public void doCollectInformation(@NotNull ProgressIndicator progress) {
    if (!EditorActivityManager.getInstance().isFocused(myEditor)) return;
    TemplateState state = TemplateManagerImpl.getTemplateState(myEditor);
    if (state != null && !state.isFinished()) return;
    getActionsToShow(myEditor, myFile, myIntentionsInfo, myPassIdToShowIntentionsFor, myQueryIntentionActions);
    myCachedIntentions = IntentionsUI.getInstance(myProject).getCachedIntentions(myEditor, myFile);
    myActionsChanged = myCachedIntentions.wrapAndUpdateActions(myIntentionsInfo, false);
  }

  @Override
  public void doApplyInformationToEditor() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    CachedIntentions cachedIntentions = myCachedIntentions;
    boolean actionsChanged = myActionsChanged;
    TemplateState state = TemplateManagerImpl.getTemplateState(myEditor);
    if ((state == null || state.isFinished()) && cachedIntentions != null) {
      IntentionsInfo syncInfo = new IntentionsInfo();
      getActionsToShowSync(myEditor, myFile, syncInfo);
      actionsChanged |= cachedIntentions.addActions(syncInfo);

      IntentionsUI.getInstance(myProject).update(cachedIntentions, actionsChanged);
    }
  }


  /**
   * Returns the list of actions to show in the Alt-Enter popup at the caret offset in the given editor.
   *
   * @param includeSyncActions whether EDT-only providers should be queried, if {@code true}, this method should be invoked in EDT
   */
  public static @NotNull IntentionsInfo getActionsToShow(@NotNull Editor hostEditor, @NotNull PsiFile hostFile, boolean includeSyncActions) {
    IntentionsInfo result = new IntentionsInfo();
    getActionsToShow(hostEditor, hostFile, result, -1);
    if (includeSyncActions) {
      getActionsToShowSync(hostEditor, hostFile, result);
    }
    return result;
  }

  /**
   * Collects intention actions from providers intended to be invoked in EDT.
   */
  @ApiStatus.Internal
  public static void getActionsToShowSync(@NotNull Editor hostEditor,
                                          @NotNull PsiFile hostFile,
                                          @NotNull IntentionsInfo intentions) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    EditorNotificationActions.collectActions(hostEditor, intentions);
    intentions.filterActions(hostFile);
  }

  /**
   * Collects intention actions from providers intended to be invoked in a background thread.
   */
  public static void getActionsToShow(final @NotNull Editor hostEditor,
                                      final @NotNull PsiFile hostFile,
                                      final @NotNull IntentionsInfo intentions,
                                      int passIdToShowIntentionsFor) {
    getActionsToShow(hostEditor, hostFile, intentions, passIdToShowIntentionsFor, true);
  }
  private static void getActionsToShow(final @NotNull Editor hostEditor,
                                       final @NotNull PsiFile hostFile,
                                       final @NotNull IntentionsInfo intentions,
                                       int passIdToShowIntentionsFor,
                                       boolean queryIntentionActions) {
    int offset = hostEditor.getCaretModel().getOffset();
    final PsiElement psiElement = hostFile.findElementAt(offset);
    if (psiElement != null) PsiUtilCore.ensureValid(psiElement);

    intentions.setOffset(offset);

    List<HighlightInfo.IntentionActionDescriptor> fixes = new ArrayList<>();
    DaemonCodeAnalyzerImpl.HighlightByOffsetProcessor highestPriorityInfoFinder = new DaemonCodeAnalyzerImpl.HighlightByOffsetProcessor(true);
    CommonProcessors.CollectProcessor<HighlightInfo> infos = new CommonProcessors.CollectProcessor<>();
    DaemonCodeAnalyzerImpl.processHighlightsNearOffset(
      hostEditor.getDocument(), hostFile.getProject(), HighlightSeverity.INFORMATION, offset, true, infos);
    for (HighlightInfo info : infos.getResults()) {
      addAvailableFixesForGroups(info, hostEditor, hostFile, fixes, passIdToShowIntentionsFor, offset);
      highestPriorityInfoFinder.process(info);
    }

    @Nullable HighlightInfo infoAtCursor = highestPriorityInfoFinder.getResult();
    if (infoAtCursor == null) {
      intentions.errorFixesToShow.addAll(fixes);
    }
    else {
      fillIntentionsInfoForHighlightInfo(infoAtCursor, intentions, fixes);
    }

    ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();

    if (queryIntentionActions) {
      PsiFile injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(hostFile, offset);
      for (IntentionAction action : IntentionManager.getInstance().getAvailableIntentions()) {
        if (indicator != null) {
          indicator.setText(action.getFamilyName());
        }
        Pair<PsiFile, Editor> place =
          ShowIntentionActionsHandler.chooseBetweenHostAndInjected(hostFile, hostEditor, injectedFile,
                     (psiFile, editor) -> ShowIntentionActionsHandler.availableFor(psiFile, editor, action));

        if (place != null) {
          List<IntentionAction> enableDisableIntentionAction = new ArrayList<>();
          enableDisableIntentionAction.add(new EnableDisableIntentionAction(action));
          enableDisableIntentionAction.add(new EditIntentionSettingsAction(action));
          HighlightInfo.IntentionActionDescriptor descriptor =
            new HighlightInfo.IntentionActionDescriptor(action, enableDisableIntentionAction, null);
          if (!fixes.contains(descriptor)) {
            intentions.intentionsToShow.add(descriptor);
          }
        }
      }

      if (indicator != null) {
        indicator.setText(CodeInsightBundle.message("progress.text.searching.for.additional.intention.actions.quick.fixes"));
      }
      for (IntentionMenuContributor extension : IntentionMenuContributor.EP_NAME.getExtensionList()) {
        try {
          extension.collectActions(hostEditor, hostFile, intentions, passIdToShowIntentionsFor, offset);
        }
        catch (IntentionPreviewUnsupportedOperationException e) {
          //can collect action on a mock memory editor and produce exceptions - ignore
        }
      }
    }

    intentions.filterActions(hostFile);
  }

  public static void fillIntentionsInfoForHighlightInfo(@NotNull HighlightInfo infoAtCursor,
                                                        @NotNull IntentionsInfo intentions,
                                                        @NotNull List<? extends HighlightInfo.IntentionActionDescriptor> fixes) {
    final boolean isError = infoAtCursor.getSeverity() == HighlightSeverity.ERROR;
    for (HighlightInfo.IntentionActionDescriptor fix : fixes) {
      if (fix.isError() && isError) {
        intentions.errorFixesToShow.add(fix);
      }
      else if (fix.isInformation()) {
        intentions.intentionsToShow.add(fix);
      }
      else {
        intentions.inspectionFixesToShow.add(fix);
      }
    }
  }
}

