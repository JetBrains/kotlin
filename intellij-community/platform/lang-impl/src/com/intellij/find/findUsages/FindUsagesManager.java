// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.findUsages;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.FindBundle;
import com.intellij.find.FindSettings;
import com.intellij.find.findUsages.FindUsagesHandlerFactory.OperationMode;
import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.search.*;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.usages.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * see {@link com.intellij.find.impl.FindManagerImpl#getFindUsagesManager()}
 */
public class FindUsagesManager {
  private static final Logger LOG = Logger.getInstance("#com.intellij.find.findParameterUsages.FindUsagesManager");

  private enum FileSearchScope {
    FROM_START,
    FROM_END,
    AFTER_CARET,
    BEFORE_CARET
  }

  private static final Key<String> KEY_START_USAGE_AGAIN = Key.create("KEY_START_USAGE_AGAIN");
  @NonNls private static final String VALUE_START_USAGE_AGAIN = "START_AGAIN";
  private final Project myProject;
  private final UsageViewManager myAnotherManager;

  private PsiElement2UsageTargetComposite myLastSearchInFileData; // EDT only
  private final UsageHistory myHistory = new UsageHistory();

  public FindUsagesManager(@NotNull Project project, @NotNull UsageViewManager anotherManager) {
    myProject = project;
    myAnotherManager = anotherManager;
  }

  public boolean canFindUsages(@NotNull final PsiElement element) {
    for (FindUsagesHandlerFactory factory : FindUsagesHandlerFactory.EP_NAME.getExtensions(myProject)) {
      try {
        if (factory.canFindUsages(element)) {
          return true;
        }
      }
      catch (IndexNotReadyException | ProcessCanceledException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
    return false;
  }

  public void clearFindingNextUsageInFile() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myLastSearchInFileData = null;
  }

  public boolean findNextUsageInFile(@NotNull Editor editor) {
    return findUsageInFile(editor, FileSearchScope.AFTER_CARET);
  }

  public boolean findPreviousUsageInFile(@NotNull Editor editor) {
    return findUsageInFile(editor, FileSearchScope.BEFORE_CARET);
  }

  private boolean findUsageInFile(@NotNull Editor editor, @NotNull FileSearchScope direction) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (myLastSearchInFileData == null) return false;
    PsiElement[] primaryElements = myLastSearchInFileData.getPrimaryElements();
    PsiElement[] secondaryElements = myLastSearchInFileData.getSecondaryElements();
    if (primaryElements.length == 0) {//all elements have been invalidated
        Messages.showMessageDialog(myProject, FindBundle.message("find.searched.elements.have.been.changed.error"),
                                   FindBundle.message("cannot.search.for.usages.title"), Messages.getInformationIcon());
        // SCR #10022
        //clearFindingNextUsageInFile();
        return false;
    }

    Document document = editor.getDocument();
    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (psiFile == null) return false;

    final FindUsagesHandler handler = getFindUsagesHandler(primaryElements[0], false);
    if (handler == null) return false;
    findUsagesInEditor(primaryElements, secondaryElements, handler, psiFile, direction, myLastSearchInFileData.myOptions, editor);
    return true;
  }


  private void initLastSearchElement(@NotNull FindUsagesOptions findUsagesOptions,
                                     @NotNull PsiElement[] primaryElements,
                                     @NotNull PsiElement[] secondaryElements) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    myLastSearchInFileData = new PsiElement2UsageTargetComposite(primaryElements, secondaryElements, findUsagesOptions);
  }

  @Nullable
  public FindUsagesHandler getFindUsagesHandler(@NotNull PsiElement element, final boolean forHighlightUsages) {
    return getFindUsagesHandler(element, forHighlightUsages ? OperationMode.HIGHLIGHT_USAGES : OperationMode.DEFAULT);
  }

  @Nullable
  public FindUsagesHandler getFindUsagesHandler(@NotNull PsiElement element, @NotNull OperationMode operationMode) {
    for (FindUsagesHandlerFactory factory : FindUsagesHandlerFactory.EP_NAME.getExtensions(myProject)) {
      if (factory.canFindUsages(element)) {
        final FindUsagesHandler handler = factory.createFindUsagesHandler(element, operationMode);
        if (handler == FindUsagesHandler.NULL_HANDLER) return null;
        if (handler != null) {
          return handler;
        }
      }
    }
    return null;
  }

  @Nullable
  public FindUsagesHandler getNewFindUsagesHandler(@NotNull PsiElement element, final boolean forHighlightUsages) {
    for (FindUsagesHandlerFactory factory : FindUsagesHandlerFactory.EP_NAME.getExtensions(myProject)) {
      if (factory.canFindUsages(element)) {
        Class<? extends FindUsagesHandlerFactory> aClass = factory.getClass();
        FindUsagesHandlerFactory copy = (FindUsagesHandlerFactory)new CachingConstructorInjectionComponentAdapter(aClass.getName(), aClass)
          .getComponentInstance(myProject.getPicoContainer());
        final FindUsagesHandler handler = copy.createFindUsagesHandler(element, forHighlightUsages);
        if (handler == FindUsagesHandler.NULL_HANDLER) return null;
        if (handler != null) {
          return handler;
        }
      }
    }
    return null;
  }

  public void findUsages(@NotNull PsiElement psiElement, @Nullable PsiFile scopeFile, final FileEditor editor, boolean showDialog, @Nullable("null means default (stored in options)") SearchScope searchScope) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    FindUsagesHandler handler = getFindUsagesHandler(psiElement, showDialog ? OperationMode.DEFAULT : OperationMode.USAGES_WITH_DEFAULT_OPTIONS);
    if (handler == null) return;

    boolean singleFile = scopeFile != null;
    AbstractFindUsagesDialog dialog = handler.getFindUsagesDialog(singleFile, shouldOpenInNewTab(), mustOpenInNewTab());
    if (showDialog) {
      if (!dialog.showAndGet()) {
        return;
      }
    }
    else {
      dialog.close(DialogWrapper.OK_EXIT_CODE);
    }

    setOpenInNewTab(dialog.isShowInSeparateWindow());

    FindUsagesOptions findUsagesOptions = dialog.calcFindUsagesOptions();
    if (searchScope != null)  {
      findUsagesOptions.searchScope = searchScope;
    }

    clearFindingNextUsageInFile();

    startFindUsages(findUsagesOptions, handler, scopeFile, editor);
  }

  void startFindUsages(@NotNull PsiElement psiElement, @NotNull FindUsagesOptions findUsagesOptions) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    FindUsagesHandler handler = getFindUsagesHandler(psiElement, false);
    if (handler == null) return;
    startFindUsages(findUsagesOptions, handler, null, null);
  }

  private void startFindUsages(@NotNull FindUsagesOptions findUsagesOptions,
                               @NotNull FindUsagesHandler handler,
                               PsiFile scopeFile,
                               FileEditor fileEditor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    boolean singleFile = scopeFile != null;

    clearFindingNextUsageInFile();
    LOG.assertTrue(handler.getPsiElement().isValid());
    PsiElement[] primaryElements = handler.getPrimaryElements();
    checkNotNull(primaryElements, handler, "getPrimaryElements()");
    PsiElement[] secondaryElements = handler.getSecondaryElements();
    checkNotNull(secondaryElements, handler, "getSecondaryElements()");
    if (singleFile && fileEditor instanceof TextEditor) {
      Editor editor = ((TextEditor)fileEditor).getEditor();
      editor.putUserData(KEY_START_USAGE_AGAIN, null);
      findUsagesInEditor(primaryElements, secondaryElements, handler, scopeFile, FileSearchScope.FROM_START, findUsagesOptions.clone(),
                         editor);
    }
    else {
      boolean skipResultsWithOneUsage = FindSettings.getInstance().isSkipResultsWithOneUsage();
      findUsages(primaryElements, secondaryElements, handler, findUsagesOptions, skipResultsWithOneUsage);
    }
  }

  public static void showSettingsAndFindUsages(@NotNull NavigationItem[] targets) {
    if (targets.length == 0) return;
    NavigationItem target = targets[0];
    if (!(target instanceof ConfigurableUsageTarget)) return;
    ((ConfigurableUsageTarget)target).showSettings();
  }

  private static void checkNotNull(@NotNull PsiElement[] elements,
                                   @NotNull FindUsagesHandler handler,
                                   @NonNls @NotNull String methodName) {
    for (PsiElement element : elements) {
      if (element == null) {
        LOG.error(handler + "." + methodName + " has returned array with null elements: " + Arrays.asList(elements));
      }
    }
  }

  @NotNull
  public static ProgressIndicator startProcessUsages(@NotNull final FindUsagesHandler handler,
                                                     @NotNull final PsiElement[] primaryElements,
                                                     @NotNull final PsiElement[] secondaryElements,
                                                     @NotNull final Processor<Usage> processor,
                                                     @NotNull final FindUsagesOptions findUsagesOptions,
                                                     @NotNull final Runnable onComplete) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final ProgressIndicatorBase indicator = new ProgressIndicatorBase();
    Task.Backgroundable task = new Task.Backgroundable(handler.getProject(), "Finding Usages") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        UsageSearcher usageSearcher = ReadAction.compute(()-> {
          PsiElement2UsageTargetAdapter[] primaryTargets = PsiElement2UsageTargetAdapter.convert(primaryElements);
          PsiElement2UsageTargetAdapter[] secondaryTargets = PsiElement2UsageTargetAdapter.convert(secondaryElements);
          return createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, null);
        });
        usageSearcher.generate(processor);
      }
    };

    ((ProgressManagerImpl)ProgressManager.getInstance()).runProcessWithProgressAsynchronously(task, indicator, onComplete);

    return indicator;
  }

  @NotNull
  public UsageViewPresentation createPresentation(@NotNull FindUsagesHandler handler, @NotNull FindUsagesOptions findUsagesOptions) {
    PsiElement element = handler.getPsiElement();
    LOG.assertTrue(element.isValid());
    return createPresentation(element, findUsagesOptions, FindSettings.getInstance().isShowResultsInSeparateView());
  }

  private void setOpenInNewTab(final boolean toOpenInNewTab) {
    if (!mustOpenInNewTab()) {
      FindSettings.getInstance().setShowResultsInSeparateView(toOpenInNewTab);
    }
  }

  private boolean shouldOpenInNewTab() {
    return mustOpenInNewTab() || FindSettings.getInstance().isShowResultsInSeparateView();
  }

  private boolean mustOpenInNewTab() {
    Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
    return selectedContent != null && selectedContent.isPinned();
  }


  /**
   * @throws PsiInvalidElementAccessException when the searcher can't be created (i.e. because element was invalidated)
   */
  @NotNull
  private static UsageSearcher createUsageSearcher(@NotNull final PsiElement2UsageTargetAdapter[] primaryTargets,
                                                   @NotNull final PsiElement2UsageTargetAdapter[] secondaryTargets,
                                                   @NotNull final FindUsagesHandler handler,
                                                   @NotNull FindUsagesOptions options,
                                                   final PsiFile scopeFile) throws PsiInvalidElementAccessException {
    ReadAction.run(() -> {
      PsiElement[] primaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets);
      PsiElement[] secondaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets);

      ContainerUtil
        .concat(primaryElements, secondaryElements)
        .forEach(psi -> {
          if (psi == null || !psi.isValid()) throw new PsiInvalidElementAccessException(psi);
        });
    });

    FindUsagesOptions optionsClone = options.clone();
    return processor -> {
      PsiElement[] primaryElements = ReadAction.compute(() -> PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets));
      PsiElement[] secondaryElements = ReadAction.compute(() -> PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets));

      Project project = ReadAction.compute(() -> scopeFile != null ? scopeFile.getProject() : primaryElements[0].getProject());
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      LOG.assertTrue(indicator != null, "Must run under progress. see ProgressManager.run*");

      ((PsiManagerImpl)PsiManager.getInstance(project)).dropResolveCacheRegularly(indicator);

      if (scopeFile != null) {
        optionsClone.searchScope = new LocalSearchScope(scopeFile);
      }
      final Processor<UsageInfo> usageInfoProcessor = new CommonProcessors.UniqueProcessor<>(usageInfo -> {
        Usage usage = ReadAction.compute(() -> UsageInfoToUsageConverter.convert(primaryElements, usageInfo));
        return processor.process(usage);
      });
      final Iterable<PsiElement> elements = ContainerUtil.concat(primaryElements, secondaryElements);

      optionsClone.fastTrack = new SearchRequestCollector(new SearchSession());
      if (optionsClone.searchScope instanceof GlobalSearchScope) {
        // we will search in project scope always but warn if some usage is out of scope
        optionsClone.searchScope = optionsClone.searchScope.union(GlobalSearchScope.projectScope(project));
      }
      try {
        for (final PsiElement element : elements) {
          if (!handler.processElementUsages(element, usageInfoProcessor, optionsClone)) {
            return;
          }
          
          for (CustomUsageSearcher searcher : CustomUsageSearcher.EP_NAME.getExtensionList()) {
            try {
              searcher.processElementUsages(element, processor, optionsClone);
            }
            catch (IndexNotReadyException e) {
              DumbService.getInstance(element.getProject()).showDumbModeNotification("Find usages is not available during indexing");
            }
            catch (ProcessCanceledException e) {
              throw e;
            }
            catch (Exception e) {
              LOG.error(e);
            }
          }
        }

        PsiSearchHelper.getInstance(project)
          .processRequests(optionsClone.fastTrack, ref -> {
            UsageInfo info = ReadAction.compute(() -> {
              if (!ref.getElement().isValid()) return null;
              return new UsageInfo(ref);
            });
            return info == null || usageInfoProcessor.process(info);
          });
      }
      finally {
        optionsClone.fastTrack = null;
      }
    };
  }

  @NotNull
  private static PsiElement2UsageTargetAdapter[] convertToUsageTargets(@NotNull Iterable<? extends PsiElement> elementsToSearch,
                                                                       @NotNull final FindUsagesOptions findUsagesOptions) {
    final List<PsiElement2UsageTargetAdapter> targets = ContainerUtil.map(elementsToSearch,
                                                                          element -> convertToUsageTarget(element, findUsagesOptions));
    return targets.toArray(new PsiElement2UsageTargetAdapter[0]);
  }

  public void findUsages(@NotNull final PsiElement[] primaryElements,
                         @NotNull final PsiElement[] secondaryElements,
                         @NotNull final FindUsagesHandler handler,
                         @NotNull final FindUsagesOptions findUsagesOptions,
                         final boolean toSkipUsagePanelWhenOneUsage) {
    doFindUsages(primaryElements, secondaryElements, handler, findUsagesOptions, toSkipUsagePanelWhenOneUsage);
  }

  public UsageView doFindUsages(@NotNull final PsiElement[] primaryElements,
                                @NotNull final PsiElement[] secondaryElements,
                                @NotNull final FindUsagesHandler handler,
                                @NotNull final FindUsagesOptions findUsagesOptions,
                                final boolean toSkipUsagePanelWhenOneUsage) {
    if (primaryElements.length == 0) {
      throw new AssertionError(handler + " " + findUsagesOptions);
    }
    PsiElement2UsageTargetAdapter[] primaryTargets = convertToUsageTargets(Arrays.asList(primaryElements), findUsagesOptions);
    PsiElement2UsageTargetAdapter[] secondaryTargets = convertToUsageTargets(Arrays.asList(secondaryElements), findUsagesOptions);
    PsiElement2UsageTargetAdapter[] targets = ArrayUtil.mergeArrays(primaryTargets, secondaryTargets);
    Factory<UsageSearcher> factory = () -> createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, null);
    UsageView usageView = myAnotherManager.searchAndShowUsages(targets,
                                                               factory, !toSkipUsagePanelWhenOneUsage,
                                                               true,
                                                               createPresentation(primaryElements[0], findUsagesOptions, shouldOpenInNewTab()),
                                                               null);
    myHistory.add(targets[0]);
    return usageView;
  }

  @NotNull
  private static UsageViewPresentation createPresentation(@NotNull PsiElement psiElement,
                                                          @NotNull FindUsagesOptions options,
                                                          boolean toOpenInNewTab) {
    UsageViewPresentation presentation = new UsageViewPresentation();
    String scopeString = options.searchScope.getDisplayName();
    presentation.setScopeText(scopeString);
    String usagesString = generateUsagesString(options);
    presentation.setUsagesString(usagesString);
    String title = FindBundle.message("find.usages.of.element.in.scope.panel.title", usagesString, UsageViewUtil.getLongName(psiElement),
                         scopeString);
    presentation.setTabText(title);
    presentation.setTabName(FindBundle.message("find.usages.of.element.tab.name", usagesString, UsageViewUtil.getShortName(psiElement)));
    presentation.setTargetsNodeText(StringUtil.capitalize(UsageViewUtil.getType(psiElement)));
    presentation.setOpenInNewTab(toOpenInNewTab);
    return presentation;
  }

  private void findUsagesInEditor(@NotNull final PsiElement[] primaryElements,
                                  @NotNull final PsiElement[] secondaryElements,
                                  @NotNull FindUsagesHandler handler,
                                  @NotNull PsiFile scopeFile,
                                  @NotNull FileSearchScope direction,
                                  @NotNull final FindUsagesOptions findUsagesOptions,
                                  @NotNull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    initLastSearchElement(findUsagesOptions, primaryElements, secondaryElements);

    clearStatusBar();

    PsiElement2UsageTargetAdapter[] primaryTargets = PsiElement2UsageTargetAdapter.convert(primaryElements);
    PsiElement2UsageTargetAdapter[] secondaryTargets = PsiElement2UsageTargetAdapter.convert(primaryElements);
    final UsageSearcher usageSearcher = createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, scopeFile);
    AtomicBoolean usagesWereFound = new AtomicBoolean();

    int startOffset = editor.getCaretModel().getOffset();

    new Task.Backgroundable(myProject, FindBundle.message("find.progress.searching.message", "editor")){
      private Usage myUsage;

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        myUsage = findSiblingUsage(usageSearcher, direction, startOffset, usagesWereFound, editor);
      }

      @Override
      public void onFinished() {
        if (FindUsagesManager.this.myProject.isDisposed() || editor.isDisposed()) return;
        if (myUsage != null) {
          myUsage.navigate(true);
          myUsage.selectInEditor();
        }
        else if (!usagesWereFound.get()) {
          String message = getNoUsagesFoundMessage(primaryElements[0]) + " in " + scopeFile.getName();
          showEditorHint(message, editor);
        }
        else {
          editor.putUserData(KEY_START_USAGE_AGAIN, VALUE_START_USAGE_AGAIN);
          showEditorHint(getSearchAgainMessage(primaryElements[0], direction), editor);
        }
      }
    }.queue();
  }

  @NotNull
  private static String getNoUsagesFoundMessage(@NotNull PsiElement psiElement) {
    String elementType = UsageViewUtil.getType(psiElement);
    String elementName = UsageViewUtil.getShortName(psiElement);
    return FindBundle.message("find.usages.of.element_type.element_name.not.found.message", elementType, elementName);
  }

  private void clearStatusBar() {
    StatusBar.Info.set("", myProject);
  }

  @NotNull
  private static String getSearchAgainMessage(@NotNull PsiElement element, @NotNull FileSearchScope direction) {
    String message = getNoUsagesFoundMessage(element);
    if (direction == FileSearchScope.AFTER_CARET) {
      AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT);
      String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
      if (shortcutsText.isEmpty()) {
        message = FindBundle.message("find.search.again.from.top.action.message", message);
      }
      else {
        message = FindBundle.message("find.search.again.from.top.hotkey.message", message, shortcutsText);
      }
    }
    else {
      String shortcutsText =
        KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_PREVIOUS));
      if (shortcutsText.isEmpty()) {
        message = FindBundle.message("find.search.again.from.bottom.action.message", message);
      }
      else {
        message = FindBundle.message("find.search.again.from.bottom.hotkey.message", message, shortcutsText);
      }
    }
    return message;
  }

  private static Usage findSiblingUsage(@NotNull final UsageSearcher usageSearcher,
                                        @NotNull FileSearchScope dir,
                                        int startOffset,
                                        @NotNull final AtomicBoolean usagesWereFound,
                                        @NotNull Editor editor) {
    if (editor.getUserData(KEY_START_USAGE_AGAIN) != null) {
      dir = dir == FileSearchScope.AFTER_CARET ? FileSearchScope.FROM_START : FileSearchScope.FROM_END;
    }

    final FileSearchScope direction = dir;

    final AtomicReference<Usage> foundUsage = new AtomicReference<>();
    usageSearcher.generate(usage -> {
      usagesWereFound.set(true);
      int usageOffset = usage instanceof UsageInfo2UsageAdapter ? ((UsageInfo2UsageAdapter)usage).getNavigationRange().getStartOffset() : 0;
      switch (direction) {
        case FROM_START:
          foundUsage.compareAndSet(null, usage);
          return false;
        case FROM_END:
          foundUsage.set(usage);
          break;
        case AFTER_CARET:
          if (usageOffset > startOffset) {
            foundUsage.set(usage);
            return false;
          }
          break;
        case BEFORE_CARET:
          if (usageOffset >= startOffset) {
            return false;
          }
          while (true) {
            Usage found = foundUsage.get();
            if (found == null) {
              if (foundUsage.compareAndSet(null, usage)) break;
            }
            else {
              int foundOffset = found instanceof UsageInfo2UsageAdapter ? ((UsageInfo2UsageAdapter)found).getNavigationRange().getStartOffset() : 0;
              if (foundOffset < usageOffset && foundUsage.compareAndSet(found, usage)) break;
            }
          }
          break;
      }

      return true;
    });

    editor.putUserData(KEY_START_USAGE_AGAIN, null);

    return foundUsage.get();
  }

  @NotNull
  private static PsiElement2UsageTargetAdapter convertToUsageTarget(@NotNull PsiElement elementToSearch,
                                                                    @NotNull FindUsagesOptions findUsagesOptions) {
    if (elementToSearch instanceof NavigationItem) {
      return new PsiElement2UsageTargetAdapter(elementToSearch,findUsagesOptions);
    }
    throw new IllegalArgumentException("Wrong usage target:" + elementToSearch + "; " + elementToSearch.getClass());
  }

  @NotNull
  private static String generateUsagesString(@NotNull FindUsagesOptions selectedOptions) {
    return selectedOptions.generateUsagesString();
  }

  private static void showEditorHint(@NotNull String message, @NotNull Editor editor) {
    JComponent component = HintUtil.createInformationLabel(message);
    final LightweightHint hint = new LightweightHint(component);
    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, HintManager.UNDER,
                                                     HintManager.HIDE_BY_ANY_KEY |
                                                     HintManager.HIDE_BY_TEXT_CHANGE |
                                                     HintManager.HIDE_BY_SCROLLING, 0, false);
  }

  public static String getHelpID(@NotNull PsiElement element) {
    return LanguageFindUsages.getHelpId(element);
  }

  public void rerunAndRecallFromHistory(@NotNull ConfigurableUsageTarget usageTarget) {
    usageTarget.findUsages();
    addToHistory(usageTarget);
  }

  public void addToHistory(@NotNull ConfigurableUsageTarget usageTarget) {
    myHistory.add(usageTarget);
  }

  @NotNull
  public UsageHistory getHistory() {
    return myHistory;
  }


  @NotNull
  public static GlobalSearchScope getMaximalScope(@NotNull FindUsagesHandler handler) {
    PsiElement element = handler.getPsiElement();
    Project project = element.getProject();
    PsiFile file = element.getContainingFile();
    if (file != null && ProjectFileIndex.SERVICE.getInstance(project).isInContent(file.getViewProvider().getVirtualFile())) {
      return GlobalSearchScope.projectScope(project);
    }
    return GlobalSearchScope.allScope(project);
  }
}
