// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.replaceInProject;

import com.intellij.find.*;
import com.intellij.find.actions.FindInPathAction;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.usages.*;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.usages.rules.UsageInFile;
import com.intellij.util.AdapterProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class ReplaceInProjectManager {
  private static final NotificationGroup NOTIFICATION_GROUP = FindInPathAction.NOTIFICATION_GROUP;

  private final Project myProject;
  private boolean myIsFindInProgress;

  public static ReplaceInProjectManager getInstance(Project project) {
    return ServiceManager.getService(project, ReplaceInProjectManager.class);
  }

  public ReplaceInProjectManager(Project project) {
    myProject = project;
  }

  private static boolean hasReadOnlyUsages(final Collection<? extends Usage> usages) {
    for (Usage usage : usages) {
      if (usage.isReadOnly()) return true;
    }

    return false;
  }

  static class ReplaceContext {
    private final UsageView usageView;
    private final FindModel findModel;
    private Set<Usage> excludedSet;

    ReplaceContext(@NotNull UsageView usageView, @NotNull FindModel findModel) {
      this.usageView = usageView;
      this.findModel = findModel;
    }

    @NotNull
    public FindModel getFindModel() {
      return findModel;
    }

    @NotNull
    public UsageView getUsageView() {
      return usageView;
    }

    @NotNull
    Set<Usage> getExcludedSetCached() {
      if (excludedSet == null) excludedSet = usageView.getExcludedUsages();
      return excludedSet;
    }

    void invalidateExcludedSetCache() {
      excludedSet = null;
    }
  }

  /**
   * @param model would be used for replacing if not null, otherwise shared (project-level) model would be used
   */
  public void replaceInProject(@NotNull DataContext dataContext, @Nullable FindModel model) {
    final FindManager findManager = FindManager.getInstance(myProject);
    final FindModel findModel;

    final boolean isOpenInNewTabEnabled;
    final boolean toOpenInNewTab;
    final Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
    if (selectedContent != null && selectedContent.isPinned()) {
      toOpenInNewTab = true;
      isOpenInNewTabEnabled = false;
    }
    else {
      toOpenInNewTab = FindSettings.getInstance().isShowResultsInSeparateView();
      isOpenInNewTabEnabled = UsageViewContentManager.getInstance(myProject).getReusableContentsCount() > 0;
    }
    if (model == null) {

      findModel = findManager.getFindInProjectModel().clone();
      findModel.setReplaceState(true);
      findModel.setOpenInNewTabEnabled(isOpenInNewTabEnabled);
      findModel.setOpenInNewTab(toOpenInNewTab);
      FindInProjectUtil.setDirectoryName(findModel, dataContext);
      FindInProjectUtil.initStringToFindFromDataContext(findModel, dataContext);
    }
    else {
      findModel = model;
      findModel.setOpenInNewTabEnabled(isOpenInNewTabEnabled);
    }

    findManager.showFindDialog(findModel, () -> {
      if (findModel.isReplaceState()) {
        replaceInPath(findModel);
      } else {
        FindInProjectManager.getInstance(myProject).findInPath(findModel);
      }
    });
  }

  public void replaceInPath(@NotNull FindModel findModel) {
    FindManager findManager = FindManager.getInstance(myProject);
    if (!findModel.isProjectScope() &&
        FindInProjectUtil.getDirectory(findModel) == null &&
        findModel.getModuleName() == null &&
        findModel.getCustomScope() == null) {
      return;
    }

    UsageViewManager manager = UsageViewManager.getInstance(myProject);

    if (manager == null) return;
    findManager.getFindInProjectModel().copyFrom(findModel);
    final FindModel findModelCopy = findModel.clone();

    final UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModelCopy);
    final FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation(myProject, true, presentation);
    processPresentation.setShowFindOptionsPrompt(findModel.isPromptOnReplace());

    UsageSearcherFactory factory = new UsageSearcherFactory(findModelCopy, processPresentation);
    searchAndShowUsages(manager, factory, findModelCopy, presentation, processPresentation);
  }

  private static class ReplaceInProjectTarget extends FindInProjectUtil.StringUsageTarget {
    ReplaceInProjectTarget(@NotNull Project project, @NotNull FindModel findModel) {
      super(project, findModel);
    }

    @NotNull
    @Override
    public String getLongDescriptiveName() {
      UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(myFindModel);
      return "Replace " + StringUtil.decapitalize(presentation.getToolwindowTitle()) + " with '" + myFindModel.getStringToReplace() + "'";
    }

    @Override
    public KeyboardShortcut getShortcut() {
      return ActionManager.getInstance().getKeyboardShortcut("ReplaceInPath");
    }

    @Override
    public void showSettings() {
      Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
      JComponent component = selectedContent == null ? null : selectedContent.getComponent();
      ReplaceInProjectManager findInProjectManager = getInstance(myProject);
      findInProjectManager.replaceInProject(DataManager.getInstance().getDataContext(component), myFindModel);
    }
  }

  public void searchAndShowUsages(@NotNull UsageViewManager manager,
                                  @NotNull Factory<UsageSearcher> usageSearcherFactory,
                                  @NotNull final FindModel findModelCopy,
                                  @NotNull UsageViewPresentation presentation,
                                  @NotNull FindUsagesProcessPresentation processPresentation) {
    presentation.setMergeDupLinesAvailable(false);
    final ReplaceInProjectTarget target = new ReplaceInProjectTarget(myProject, findModelCopy);
    ((FindManagerImpl)FindManager.getInstance(myProject)).getFindUsagesManager().addToHistory(target);
    final ReplaceContext[] context = new ReplaceContext[1];
    manager.searchAndShowUsages(new UsageTarget[]{target},
                                usageSearcherFactory, processPresentation, presentation, new UsageViewManager.UsageViewStateListener() {
        @Override
        public void usageViewCreated(@NotNull UsageView usageView) {
          context[0] = new ReplaceContext(usageView, findModelCopy);
          addReplaceActions(context[0]);
          usageView.setRerunAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
              UsageViewPresentation rerunPresentation = presentation.copy();
              rerunPresentation.setOpenInNewTab(false);
              searchAndShowUsages(manager, usageSearcherFactory, findModelCopy, rerunPresentation, processPresentation);
            }
          });
        }

        @Override
        public void findingUsagesFinished(final UsageView usageView) {
          if (context[0] != null && !processPresentation.isShowFindOptionsPrompt()) {
            TransactionGuard.submitTransaction(myProject, () -> {
              replaceUsagesUnderCommand(context[0], usageView.getUsages());
              context[0].invalidateExcludedSetCache();
            });
          }
        }
      });
  }

  public boolean showReplaceAllConfirmDialog(@NotNull String usagesCount, @NotNull String stringToFind, @NotNull String filesCount, @NotNull String stringToReplace) {
    return Messages.YES == MessageDialogBuilder.yesNo(
      FindBundle.message("find.replace.all.confirmation.title"),
      FindBundle.message("find.replace.all.confirmation", usagesCount, StringUtil.escapeXmlEntities(stringToFind), filesCount,
                         StringUtil.escapeXmlEntities(stringToReplace)))
                                               .yesText(FindBundle.message("find.replace.command"))
                                               .project(myProject)
                                               .noText(Messages.CANCEL_BUTTON).show();
  }

  private static Set<VirtualFile> getFiles(@NotNull ReplaceContext replaceContext, boolean selectedOnly) {
    Set<Usage> usages = selectedOnly
                        ? replaceContext.getUsageView().getSelectedUsages()
                        : replaceContext.getUsageView().getUsages();
    if (usages.isEmpty()) {
      return Collections.emptySet();
    }

    Set<VirtualFile> files = new HashSet<>();
    for (Usage usage : usages) {
      if (usage instanceof UsageInfo2UsageAdapter) {
        files.add(((UsageInfo2UsageAdapter)usage).getFile());
      }
    }
    return files;
  }

  private static Set<Usage> getAllUsagesForFile(@NotNull ReplaceContext replaceContext, @NotNull VirtualFile file) {
    Set<Usage> usages = replaceContext.getUsageView().getUsages();
    Set<Usage> result = new LinkedHashSet<>();
    for (Usage usage : usages) {
      if (usage instanceof UsageInfo2UsageAdapter && Comparing.equal(((UsageInfo2UsageAdapter)usage).getFile(), file)) {
        result.add(usage);
      }
    }
    return result;
  }

  private void addReplaceActions(final ReplaceContext replaceContext) {
    final AbstractAction replaceAllAction = new AbstractAction(FindBundle.message("find.replace.all.action")) {
      {
        KeyStroke altShiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        putValue(ACCELERATOR_KEY, altShiftEnter);
        putValue(SHORT_DESCRIPTION, KeymapUtil.getKeystrokeText(altShiftEnter));
      }
      @Override
      public void actionPerformed(ActionEvent e) {
        Set<Usage> usages = replaceContext.getUsageView().getUsages();
        if (usages.isEmpty()) return;
        Set<VirtualFile> files = getFiles(replaceContext, false);
        if (files.size() < 2 || showReplaceAllConfirmDialog(
          String.valueOf(usages.size()),
          replaceContext.getFindModel().getStringToFind(),
          String.valueOf(files.size()),
          replaceContext.getFindModel().getStringToReplace())) {
          replaceUsagesUnderCommand(replaceContext, usages);
        }
      }

      @Override
      public boolean isEnabled() {
        return !replaceContext.getUsageView().getUsages().isEmpty();
      }
    };
    replaceContext.getUsageView().addButtonToLowerPane(replaceAllAction);

    final AbstractAction replaceSelectedAction = new AbstractAction() {
      {
        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
        putValue(ACCELERATOR_KEY, altEnter);
        putValue(LONG_DESCRIPTION, KeymapUtil.getKeystrokeText(altEnter));
        putValue(SHORT_DESCRIPTION, KeymapUtil.getKeystrokeText(altEnter));
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        replaceUsagesUnderCommand(replaceContext, replaceContext.getUsageView().getSelectedUsages());
      }

      @Override
      public Object getValue(String key) {
        return Action.NAME.equals(key)
               ? FindBundle.message("find.replace.selected.action", replaceContext.getUsageView().getSelectedUsages().size())
               : super.getValue(key);
      }

      @Override
      public boolean isEnabled() {
        return !replaceContext.getUsageView().getSelectedUsages().isEmpty();
      }
    };

    replaceContext.getUsageView().addButtonToLowerPane(replaceSelectedAction);

    final AbstractAction replaceAllInThisFileAction = new AbstractAction() {
      {
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        Set<VirtualFile> files = getFiles(replaceContext, true);
        if (files.size() == 1) {
          replaceUsagesUnderCommand(replaceContext, getAllUsagesForFile(replaceContext, files.iterator().next()));
        }
      }

      @Override
      public Object getValue(String key) {
        return Action.NAME.equals(key)
               ? FindBundle.message("find.replace.this.file.action", replaceContext.getUsageView().getSelectedUsages().size())
               : super.getValue(key);
      }

      @Override
      public boolean isEnabled() {
        return getFiles(replaceContext, true).size() == 1;
      }
    };

    //replaceContext.getUsageView().addButtonToLowerPane(replaceAllInThisFileAction);

    final AbstractAction skipThisFileAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Set<VirtualFile> files = getFiles(replaceContext, true);
        if (files.size() != 1) return;
        VirtualFile selectedFile = files.iterator().next();
        Set<Usage> toSkip = getAllUsagesForFile(replaceContext, selectedFile);
        Usage usageToSelect = ((UsageViewImpl)replaceContext.getUsageView()).getNextToSelect(toSkip);
        replaceContext.getUsageView().excludeUsages(toSkip.toArray(Usage.EMPTY_ARRAY));
        if (usageToSelect != null) {
          replaceContext.getUsageView().selectUsages(new Usage[]{usageToSelect});
        } else {
          replaceContext.getUsageView().selectUsages(Usage.EMPTY_ARRAY);
        }
      }

      @Override
      public Object getValue(String key) {
        return Action.NAME.equals(key)
               ? FindBundle.message("find.replace.skip.this.file.action", replaceContext.getUsageView().getSelectedUsages().size())
               : super.getValue(key);
      }

      @Override
      public boolean isEnabled() {
        Set<VirtualFile> files = getFiles(replaceContext, true);
        if (files.size() != 1) return false;
        VirtualFile selectedFile = files.iterator().next();
        Set<Usage> toSkip = getAllUsagesForFile(replaceContext, selectedFile);
        return ((UsageViewImpl)replaceContext.getUsageView()).getNextToSelect(toSkip) != null;
      }
    };

    //replaceContext.getUsageView().addButtonToLowerPane(skipThisFileAction);
  }

  private boolean replaceUsages(@NotNull ReplaceContext replaceContext, @NotNull Collection<Usage> usages) {
    if (!ensureUsagesWritable(replaceContext, usages)) {
      return true;
    }

    int[] replacedCount = {0};
    final boolean[] success = {true};

    success[0] &= ((ApplicationImpl)ApplicationManager.getApplication()).runWriteActionWithCancellableProgressInDispatchThread(
      FindBundle.message("find.replace.all.confirmation.title"),
      myProject,
      null,
      indicator -> {
        indicator.setIndeterminate(false);
        int processed = 0;
        VirtualFile lastFile = null;

        for (final Usage usage : usages) {
          ++processed;
          indicator.checkCanceled();
          indicator.setFraction((float)processed / usages.size());

          if (usage instanceof UsageInFile) {
            VirtualFile virtualFile = ((UsageInFile)usage).getFile();
            if (virtualFile != null && !virtualFile.equals(lastFile)) {
              indicator.setText2(virtualFile.getPresentableUrl());
              lastFile = virtualFile;
            }
          }

          ProgressManager.getInstance().executeNonCancelableSection(() -> {
            try {
              if (replaceUsage(usage, replaceContext.getFindModel(), replaceContext.getExcludedSetCached(), false)) {
                replacedCount[0]++;
              }
            }
            catch (FindManager.MalformedReplacementStringException ex) {
              markAsMalformedReplacement(replaceContext, usage);
              success[0] = false;
            }
          });
        }
      }
    );

    replaceContext.getUsageView().removeUsagesBulk(usages);
    reportNumberReplacedOccurrences(myProject, replacedCount[0]);
    return success[0];
  }

  private static void markAsMalformedReplacement(ReplaceContext replaceContext, Usage usage) {
    replaceContext.getUsageView().excludeUsages(new Usage[]{usage});
  }

  public static void reportNumberReplacedOccurrences(Project project, int occurrences) {
    if (occurrences != 0) {
      final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
      if (statusBar != null) {
        statusBar.setInfo(FindBundle.message("0.occurrences.replaced", occurrences));
      }
    }
  }

  public boolean replaceUsage(@NotNull final Usage usage,
                              @NotNull final FindModel findModel,
                              @NotNull final Set<Usage> excludedSet,
                              final boolean justCheck)
    throws FindManager.MalformedReplacementStringException {
    final Ref<FindManager.MalformedReplacementStringException> exceptionResult = Ref.create();
    final boolean result = WriteAction.compute(() -> {
      if (excludedSet.contains(usage)) {
        return false;
      }

      final Document document = ((UsageInfo2UsageAdapter)usage).getDocument();
      if (!document.isWritable()) return false;

      return ((UsageInfo2UsageAdapter)usage).processRangeMarkers(segment -> {
        final int textOffset = segment.getStartOffset();
        final int textEndOffset = segment.getEndOffset();
        final Ref<String> stringToReplace = Ref.create();
        try {
          if (!getStringToReplace(textOffset, textEndOffset, document, findModel, stringToReplace)) return true;
          if (!stringToReplace.isNull() && !justCheck) {
            document.replaceString(textOffset, textEndOffset, stringToReplace.get());
          }
        }
        catch (FindManager.MalformedReplacementStringException e) {
          exceptionResult.set(e);
          return false;
        }
        return true;
      });
    });

    if (!exceptionResult.isNull()) {
      throw exceptionResult.get();
    }
    return result;
  }

  private boolean getStringToReplace(int textOffset,
                                     int textEndOffset,
                                     Document document, FindModel findModel, Ref<? super String> stringToReplace)
    throws FindManager.MalformedReplacementStringException {
    if (textOffset < 0 || textOffset >= document.getTextLength()) {
      return false;
    }
    if (textEndOffset < 0 || textEndOffset > document.getTextLength()) {
      return false;
    }
    FindManager findManager = FindManager.getInstance(myProject);
    final CharSequence foundString = document.getCharsSequence().subSequence(textOffset, textEndOffset);
    PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    FindResult findResult =
      findManager.findString(document.getCharsSequence(), textOffset, findModel, file != null ? file.getVirtualFile() : null);
    if (!findResult.isStringFound() ||
        // find result should be in needed range
        !(findResult.getStartOffset() >= textOffset && findResult.getEndOffset() <= textEndOffset)) {
      return false;
    }

    stringToReplace.set(
      FindManager.getInstance(myProject).getStringToReplace(foundString.toString(), findModel, textOffset, document.getText()));

    return true;
  }

  private void replaceUsagesUnderCommand(@NotNull final ReplaceContext replaceContext, @NotNull final Set<? extends Usage> usagesSet) {
    if (usagesSet.isEmpty()) {
      return;
    }

    final List<Usage> usages = new ArrayList<>(usagesSet);
    Collections.sort(usages, UsageViewImpl.USAGE_COMPARATOR);

    if (!ensureUsagesWritable(replaceContext, usages)) return;

    CommandProcessor.getInstance().executeCommand(myProject, () -> {
      final boolean success = replaceUsages(replaceContext, usages);
      final UsageView usageView = replaceContext.getUsageView();

      if (closeUsageViewIfEmpty(usageView, success)) return;
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(usageView.getPreferredFocusableComponent(), true));
    }, FindBundle.message("find.replace.command"), null);

    replaceContext.invalidateExcludedSetCache();
  }

  private boolean ensureUsagesWritable(ReplaceContext replaceContext, Collection<? extends Usage> selectedUsages) {
    Set<VirtualFile> readOnlyFiles = null;
    for (final Usage usage : selectedUsages) {
      final VirtualFile file = ((UsageInFile)usage).getFile();

      if (file != null && !file.isWritable()) {
        if (readOnlyFiles == null) readOnlyFiles = new HashSet<>();
        readOnlyFiles.add(file);
      }
    }

    if (readOnlyFiles != null) {
      ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(readOnlyFiles);
    }

    if (hasReadOnlyUsages(selectedUsages)) {
      int result = Messages.showOkCancelDialog(replaceContext.getUsageView().getComponent(),
                                               FindBundle.message("find.replace.occurrences.in.read.only.files.prompt"),
                                               FindBundle.message("find.replace.occurrences.in.read.only.files.title"),
                                               Messages.getWarningIcon());
      if (result != Messages.OK) {
        return false;
      }
    }
    return true;
  }

  private boolean closeUsageViewIfEmpty(UsageView usageView, boolean success) {
    if (usageView.getUsages().isEmpty()) {
      usageView.close();
      return true;
    }
    if (!success) {
      NOTIFICATION_GROUP.createNotification("One or more malformed replacement strings", MessageType.ERROR).notify(myProject);
    }
    return false;
  }

  public boolean isWorkInProgress() {
    return myIsFindInProgress;
  }

  public boolean isEnabled() {
    return !myIsFindInProgress && !FindInProjectManager.getInstance(myProject).isWorkInProgress();
  }

  private class UsageSearcherFactory implements Factory<UsageSearcher> {
    private final FindModel myFindModelCopy;
    private final FindUsagesProcessPresentation myProcessPresentation;

    private UsageSearcherFactory(@NotNull FindModel findModelCopy,
                                 @NotNull FindUsagesProcessPresentation processPresentation) {
      myFindModelCopy = findModelCopy;
      myProcessPresentation = processPresentation;
    }

    @Override
    public UsageSearcher create() {
      return processor -> {
        try {
          myIsFindInProgress = true;

          FindInProjectUtil.findUsages(myFindModelCopy, myProject,
                                       new AdapterProcessor<>(processor, UsageInfo2UsageAdapter.CONVERTER),
                                       myProcessPresentation);
        }
        finally {
          myIsFindInProgress = false;
        }
      };
    }
  }
}
