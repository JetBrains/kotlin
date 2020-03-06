// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.FindBundle;
import com.intellij.find.FindManager;
import com.intellij.find.FindSettings;
import com.intellij.find.UsagesPreviewPanelProvider;
import com.intellij.find.findUsages.*;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.gotoByName.ModelDiff;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.AsyncEditorLoader;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.preview.PreviewManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IntRef;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupUpdateProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.usages.*;
import com.intellij.usages.impl.*;
import com.intellij.usages.rules.UsageFilteringRuleProvider;
import com.intellij.util.ArrayUtil;
import com.intellij.util.BitUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.EdtScheduledExecutorService;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.intellij.find.actions.ShowUsagesActionHandler.getSecondInvocationTitle;
import static com.intellij.find.actions.ShowUsagesActionHandler.showUsagesInMaximalScope;

public class ShowUsagesAction extends AnAction implements PopupAction, HintManagerImpl.ActionToIgnore {
  public static final String ID = "ShowUsages";

  private static class Holder {
    private static final UsageNode USAGES_OUTSIDE_SCOPE_NODE = new UsageNode(null, ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR);
    private static final UsageNode MORE_USAGES_SEPARATOR_NODE = UsageViewImpl.NULL_NODE;

    private static final Comparator<UsageNode> USAGE_NODE_COMPARATOR = (c1, c2) -> {
      if (c1 instanceof StringNode || c2 instanceof StringNode) {
        if (c1 instanceof StringNode && c2 instanceof StringNode) {
          return Comparing.compare(c1.toString(), c2.toString());
        }
        return c1 instanceof StringNode ? 1 : -1;
      }

      Usage o1 = c1.getUsage();
      Usage o2 = c2.getUsage();
      int weight1 = o1 == ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR ? 2 : o1 == ShowUsagesTable.MORE_USAGES_SEPARATOR ? 1 : 0;
      int weight2 = o2 == ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR ? 2 : o2 == ShowUsagesTable.MORE_USAGES_SEPARATOR ? 1 : 0;
      if (weight1 != weight2) return weight1 - weight2;

      if (o1 instanceof Comparable && o2 instanceof Comparable) {
        //noinspection unchecked,rawtypes
        return ((Comparable)o1).compareTo(o2);
      }

      VirtualFile v1 = UsageListCellRenderer.getVirtualFile(o1);
      VirtualFile v2 = UsageListCellRenderer.getVirtualFile(o2);
      String name1 = v1 == null ? null : v1.getName();
      String name2 = v2 == null ? null : v2.getName();
      int i = Comparing.compare(name1, name2);
      if (i != 0) return i;
      if (Comparing.equal(v1, v2)) {
        FileEditorLocation loc1 = o1.getLocation();
        FileEditorLocation loc2 = o2.getLocation();
        return Comparing.compare(loc1, loc2);
      }
      else {
        String path1 = v1 == null ? null : v1.getPath();
        String path2 = v2 == null ? null : v2.getPath();
        return Comparing.compare(path1, path2);
      }
    };
  }

  public static int getUsagesPageSize() {
    return Math.max(1, Registry.intValue("ide.usages.page.size", 100));
  }

  public ShowUsagesAction() {
    setInjectedContext(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    FindUsagesInFileAction.updateFindUsagesAction(e);

    if (e.getPresentation().isEnabled()) {
      UsageTarget[] usageTargets = e.getData(UsageView.USAGE_TARGETS_KEY);
      if (usageTargets != null && !(ArrayUtil.getFirstElement(usageTargets) instanceof PsiElementUsageTarget)) {
        e.getPresentation().setEnabled(false);
      }
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    ShowUsagesActionState state = getState(project);
    Runnable continuation = state.continuation;
    if (continuation != null) {
      state.continuation = null;
      hideHints(); // This action is invoked when the hint is showing because it implements HintManagerImpl.ActionToIgnore
      continuation.run();
      return;
    }

    RelativePoint popupPosition = JBPopupFactory.getInstance().guessBestPopupLocation(e.getDataContext());
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.usages");

    UsageTarget[] usageTargets = e.getData(UsageView.USAGE_TARGETS_KEY);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (usageTargets == null) {
      FindUsagesAction.chooseAmbiguousTargetAndPerform(project, editor, element -> {
        startFindUsages(element, popupPosition, editor);
        return false;
      });
    }
    else if (ArrayUtil.getFirstElement(usageTargets) instanceof PsiElementUsageTarget) {
      PsiElement element = ((PsiElementUsageTarget)usageTargets[0]).getElement();
      if (element != null) {
        startFindUsages(element, popupPosition, editor);
      }
    }
  }

  private static void hideHints() {
    HintManager.getInstance().hideHints(HintManager.HIDE_BY_ANY_KEY, false, false);
  }

  public static void startFindUsages(@NotNull PsiElement element, @NotNull RelativePoint popupPosition, @Nullable Editor editor) {
    Project project = element.getProject();
    FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
    FindUsagesHandlerBase handler = findUsagesManager.getFindUsagesHandler(element, FindUsagesHandlerFactory.OperationMode.USAGES_WITH_DEFAULT_OPTIONS);
    if (handler == null) return;
    //noinspection deprecation
    FindUsagesOptions options = handler.getFindUsagesOptions(DataManager.getInstance().getDataContext());
    showElementUsages(editor, popupPosition, handler, options, new IntRef(0));
  }

  private static void rulesChanged(@NotNull UsageViewImpl usageView, @NotNull PingEDT pingEDT, JBPopup popup) {
    // later to make sure UsageViewImpl.rulesChanged was invoked
    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
      if ((popup == null || !popup.isDisposed()) && !usageView.isDisposed()) {
        usageView.waitForUpdateRequestsCompletion();
        if ((popup == null || !popup.isDisposed()) && !usageView.isDisposed()) {
          pingEDT.ping();
        }
      }
    }));
  }

  private static void showElementUsages(@Nullable Editor editor,
                                        @NotNull RelativePoint popupPosition,
                                        @NotNull FindUsagesHandlerBase handler,
                                        @NotNull FindUsagesOptions options,
                                        @NotNull IntRef minWidth) {
    Project project = handler.getProject();
    FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
    // show super method warning dialogs before starting finding usages
    PsiElement[] primaryElements = handler.getPrimaryElements();
    PsiElement[] secondaryElements = handler.getSecondaryElements();
    UsageSearcher usageSearcher = FindUsagesManager.createUsageSearcher(handler, primaryElements, secondaryElements, options);
    String searchString = FindBundle.message(
      "find.usages.of.element.tab.name",
      options.generateUsagesString(), UsageViewUtil.getLongName(handler.getPsiElement())
    );

    showElementUsages(
      project, editor, popupPosition, getUsagesPageSize(), minWidth,
      () -> searchString,
      usageSearcher,
      new ShowUsagesActionHandler() {

        @Override
        public boolean isValid() {
          return handler.getPsiElement().isValid();
        }

        @Override
        public @NotNull SearchScope getSelectedScope() {
          return options.searchScope;
        }

        @Override
        public @NotNull GlobalSearchScope getMaximalScope() {
          return FindUsagesManager.getMaximalScope(handler);
        }

        @Override
        public void showDialogAndShowUsages(@Nullable Editor newEditor) {
          showDialog(
            handler,
            newOptions -> showElementUsages(newEditor, popupPosition, handler, newOptions, minWidth)
          );
        }

        @Override
        public void findUsages() {
          findUsagesManager.findUsages(
            handler.getPrimaryElements(), handler.getSecondaryElements(),
            handler, options,
            FindSettings.getInstance().isSkipResultsWithOneUsage()
          );
        }

        @Override
        public void showUsagesInScope(@NotNull SearchScope searchScope) {
          FindUsagesOptions newOptions = options.clone();
          newOptions.searchScope = searchScope;
          showElementUsages(editor, popupPosition, handler, newOptions, minWidth);
        }
      }
    );
  }

  private static void showElementUsages(@NotNull Project project,
                                        @Nullable Editor editor,
                                        @NotNull RelativePoint popupPosition,
                                        int maxUsages,
                                        @NotNull IntRef minWidth,
                                        @NotNull UsageSearchPresentation presentation,
                                        @NotNull UsageSearcher usageSearcher,
                                        @NotNull ShowUsagesActionHandler actionHandler) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    UsageViewSettings usageViewSettings = UsageViewSettings.getInstance();
    ShowUsagesSettings showUsagesSettings = ShowUsagesSettings.getInstance();
    UsageViewSettings savedGlobalSettings = new UsageViewSettings();

    savedGlobalSettings.loadState(usageViewSettings);
    usageViewSettings.loadState(showUsagesSettings.getState());

    UsageViewImpl usageView = createUsageView(project);
    if (editor != null) {
      PsiReference reference = TargetElementUtil.findReference(editor);
      if (reference != null) {
        UsageInfo2UsageAdapter origin = new UsageInfo2UsageAdapter(new UsageInfo(reference));
        usageView.setOriginUsage(origin);
      }
    }

    Disposer.register(usageView, () -> {
      showUsagesSettings.applyUsageViewSettings(usageViewSettings);
      usageViewSettings.loadState(savedGlobalSettings);
    });

    final SearchScope searchScope = actionHandler.getSelectedScope();
    final AtomicInteger outOfScopeUsages = new AtomicInteger();
    ShowUsagesTable table = new ShowUsagesTable(new ShowUsagesTableCellRenderer(usageView, outOfScopeUsages, searchScope));
    AsyncProcessIcon processIcon = new AsyncProcessIcon("xxx");
    TitlePanel statusPanel = new TitlePanel();
    statusPanel.add(processIcon, BorderLayout.EAST);

    addUsageNodes(usageView.getRoot(), usageView, new ArrayList<>());

    List<Usage> usages = new ArrayList<>();
    Set<UsageNode> visibleNodes = new LinkedHashSet<>();
    table.setTableModel(new SmartList<>(createStringNode(UsageViewBundle.message("progress.searching"))));

    boolean isPreviewMode =
      Boolean.TRUE == PreviewManager.SERVICE.preview(project, UsagesPreviewPanelProvider.ID, Pair.create(usageView, table), false);
    Runnable itemChosenCallback = table.prepareTable(
      isPreviewMode,
      () -> showElementUsages(
        project, editor, popupPosition,
        maxUsages + getUsagesPageSize(), minWidth,
        presentation, usageSearcher, actionHandler
      ),
      () -> showUsagesInMaximalScope(actionHandler)
    );


    JBPopup popup = isPreviewMode ? null : createUsagePopup(project,
                                                            usageView, table, itemChosenCallback, presentation, statusPanel, minWidth,
                                                            () -> actionHandler.showDialogAndShowUsages(editor),
                                                            actionHandler);
    ProgressIndicator indicator = new ProgressIndicatorBase();
    if (popup != null && !popup.isDisposed()) {
      Disposer.register(popup, usageView);
      Disposer.register(popup, indicator::cancel);

      // show popup only if find usages takes more than 300ms, otherwise it would flicker needlessly
      EdtScheduledExecutorService.getInstance().schedule(() -> {
        if (!usageView.isDisposed()) {
          showPopupIfNeedTo(popup, popupPosition);
        }
      }, 300, TimeUnit.MILLISECONDS);
    }

    PingEDT pingEDT = new PingEDT("Rebuild popup in EDT", o -> popup != null && popup.isDisposed(), 100, () -> {
      if (popup != null && popup.isDisposed()) return;

      List<UsageNode> nodes = new ArrayList<>(usages.size());
      List<Usage> copy;
      synchronized (usages) {
        // open up popup as soon as the first usage has been found
        if (popup != null && !popup.isVisible() && (usages.isEmpty() || !showPopupIfNeedTo(popup, popupPosition))) {
          return;
        }
        addUsageNodes(usageView.getRoot(), usageView, nodes);
        copy = new ArrayList<>(usages);
      }

      boolean shouldShowMoreSeparator = copy.contains(ShowUsagesTable.MORE_USAGES_SEPARATOR);
      if (shouldShowMoreSeparator) {
        nodes.add(Holder.MORE_USAGES_SEPARATOR_NODE);
      }
      boolean hasOutsideScopeUsages = copy.contains(ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR);
      if (hasOutsideScopeUsages && !shouldShowMoreSeparator) {
        nodes.add(Holder.USAGES_OUTSIDE_SCOPE_NODE);
      }
      List<UsageNode> data = new ArrayList<>(nodes);
      int filteredCount = filtered(copy, usageView);
      if (filteredCount != 0) {
        data.add(createStringNode(UsageViewBundle.message("usages.were.filtered.out", filteredCount)));
      }
      data.sort(Holder.USAGE_NODE_COMPARATOR);

      boolean hasMore = shouldShowMoreSeparator || hasOutsideScopeUsages;
      int totalCount = copy.size();
      int visibleCount = totalCount - filteredCount;
      statusPanel.setText(getStatusString(!processIcon.isDisposed(), hasMore, visibleCount, totalCount));
      rebuildTable(usageView, data, table, popup, popupPosition, minWidth);
    });

    MessageBusConnection messageBusConnection = project.getMessageBus().connect(usageView);
    messageBusConnection.subscribe(UsageFilteringRuleProvider.RULES_CHANGED, () -> rulesChanged(usageView, pingEDT, popup));

    Processor<Usage> collect = usage -> {
      if (!UsageViewManagerImpl.isInScope(usage, searchScope)) {
        if (outOfScopeUsages.getAndIncrement() == 0) {
          visibleNodes.add(Holder.USAGES_OUTSIDE_SCOPE_NODE);
          usages.add(ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR);
        }
        return true;
      }
      synchronized (usages) {
        if (visibleNodes.size() >= maxUsages) return false;
        UsageNode node = ReadAction.compute(() -> usageView.doAppendUsage(usage));
        usages.add(usage);
        if (node != null) {
          visibleNodes.add(node);
          boolean continueSearch = true;
          if (visibleNodes.size() == maxUsages) {
            visibleNodes.add(Holder.MORE_USAGES_SEPARATOR_NODE);
            usages.add(ShowUsagesTable.MORE_USAGES_SEPARATOR);
            continueSearch = false;
          }
          pingEDT.ping();

          return continueSearch;
        }
      }

      return true;
    };

    FindUsagesManager.startProcessUsages(indicator, project, usageSearcher, collect, () -> ApplicationManager.getApplication().invokeLater(
      () -> {
        Disposer.dispose(processIcon);
        Container parent = processIcon.getParent();
        if (parent != null) {
          parent.remove(processIcon);
          parent.repaint();
        }
        pingEDT.ping(); // repaint status
        synchronized (usages) {
          if (visibleNodes.isEmpty()) {
            if (usages.isEmpty()) {
              String hint = UsageViewBundle.message("no.usages.found.in", searchScope.getDisplayName());
              hint(project, editor, popupPosition, false, hint, actionHandler);
              cancel(popup);
            }
            // else all usages filtered out
          }
          else if (visibleNodes.size() == 1) {
            if (usages.size() == 1) {
              //the only usage
              Usage usage = visibleNodes.iterator().next().getUsage();
              if (usage == ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR) {
                String hint = UsageViewManagerImpl.outOfScopeMessage(outOfScopeUsages.get(), searchScope);
                hint(project, editor, popupPosition, true, hint, actionHandler);
              }
              else {
                String hint = UsageViewBundle.message("show.usages.only.usage", searchScope.getDisplayName());
                navigateAndHint(project, usage, popupPosition, hint, actionHandler);
              }
              cancel(popup);
            }
            else {
              assert usages.size() > 1 : usages;
              // usage view can filter usages down to one
              Usage visibleUsage = visibleNodes.iterator().next().getUsage();
              if (areAllUsagesInOneLine(visibleUsage, usages)) {
                String hint = UsageViewBundle.message("all.usages.are.in.this.line", usages.size(), searchScope.getDisplayName());
                navigateAndHint(project, visibleUsage, popupPosition, hint, actionHandler);
                cancel(popup);
              }
            }
          }
        }
      },
      project.getDisposed()
    ));
  }

  @NotNull
  private static UsageViewImpl createUsageView(@NotNull Project project) {
    UsageViewManager manager = UsageViewManager.getInstance(project);
    UsageViewPresentation usageViewPresentation = new UsageViewPresentation();
    usageViewPresentation.setDetachedMode(true);
    return (UsageViewImpl)manager.createUsageView(
      UsageTarget.EMPTY_ARRAY, Usage.EMPTY_ARRAY, usageViewPresentation, null
    );
  }

  @NotNull
  static UsageNode createStringNode(@NotNull Object string) {
    return new StringNode(string);
  }

  private static boolean showPopupIfNeedTo(@NotNull JBPopup popup, @NotNull RelativePoint popupPosition) {
    if (!popup.isDisposed() && !popup.isVisible()) {
      popup.show(popupPosition);
      return true;
    }
    return false;
  }

  @NotNull
  private static JComponent createHintComponent(@NotNull String secondInvocationTitle, boolean isWarning, @NotNull JComponent button) {
    JComponent label = HintUtil.createInformationLabel(secondInvocationTitle);
    if (isWarning) {
      label.setBackground(MessageType.WARNING.getPopupBackground());
    }

    JPanel panel = new JPanel(new BorderLayout());
    button.setBackground(label.getBackground());
    panel.setBackground(label.getBackground());
    label.setOpaque(false);
    label.setBorder(null);
    panel.setBorder(HintUtil.createHintBorder());
    panel.add(label, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);
    return panel;
  }

  @NotNull
  private static InplaceButton createSettingsButton(@NotNull Project project,
                                                    @NotNull Runnable cancelAction,
                                                    @NotNull Runnable showDialogAndFindUsagesRunnable) {
    String shortcutText = "";
    KeyboardShortcut shortcut = UsageViewImpl.getShowUsagesWithSettingsShortcut();
    if (shortcut != null) {
      shortcutText = "(" + KeymapUtil.getShortcutText(shortcut) + ")";
    }
    return new InplaceButton("Settings..." + shortcutText, AllIcons.General.Settings, __ -> {
      ApplicationManager.getApplication().invokeLater(showDialogAndFindUsagesRunnable, project.getDisposed());
      cancelAction.run();
    });
  }

  private static void showDialog(@NotNull FindUsagesHandlerBase handler, @NotNull Consumer<? super FindUsagesOptions> optionsConsumer) {
    FUCounterUsageLogger.getInstance().logEvent("toolbar", "ShowUsagesPopup.showSettings");
    AbstractFindUsagesDialog dialog;
    if (handler instanceof FindUsagesHandlerUi) {
      dialog = ((FindUsagesHandlerUi)handler).getFindUsagesDialog(false, false, false);
    }
    else {
      dialog = FindUsagesHandler.createDefaultFindUsagesDialog(false, false, false, handler);
    }
    if (dialog.showAndGet()) {
      dialog.calcFindUsagesOptions();
      //noinspection deprecation
      FindUsagesOptions options = handler.getFindUsagesOptions(DataManager.getInstance().getDataContext());
      optionsConsumer.accept(options);
    }
  }

  @NotNull
  private static JBPopup createUsagePopup(@NotNull Project project,
                                          @NotNull UsageViewImpl usageView,
                                          @NotNull JTable table,
                                          @NotNull Runnable itemChoseCallback,
                                          @NotNull UsageSearchPresentation presentation,
                                          @NotNull TitlePanel statusPanel,
                                          @NotNull IntRef minWidth,
                                          @NotNull Runnable showDialogAndFindUsagesRunnable,
                                          @NotNull ShowUsagesActionHandler actionHandler) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    PopupChooserBuilder<?> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(table);
    String title = UsageViewBundle.message(
      "search.title.0.in.1",
      presentation.getSearchString(),
      actionHandler.getSelectedScope().getDisplayName()
    );
    builder.setTitle(XmlStringUtil.wrapInHtml("<body><nobr>" + StringUtil.escapeXmlEntities(title) + "</nobr></body>"));
    builder.setAdText(getSecondInvocationTitle(actionHandler));

    builder.setMovable(true).setResizable(true);
    builder.setItemChoosenCallback(itemChoseCallback);
    JBPopup[] popup = new JBPopup[1];

    KeyboardShortcut shortcut = UsageViewImpl.getShowUsagesWithSettingsShortcut();
    if (shortcut != null) {
      new DumbAwareAction() {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          cancel(popup[0]);
          showDialogAndFindUsagesRunnable.run();
        }
      }.registerCustomShortcutSet(new CustomShortcutSet(shortcut.getFirstKeyStroke()), table);
    }
    shortcut = getShowUsagesShortcut();
    if (shortcut != null) {
      new DumbAwareAction() {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          cancel(popup[0]);
          showUsagesInMaximalScope(actionHandler);
        }
      }.registerCustomShortcutSet(new CustomShortcutSet(shortcut.getFirstKeyStroke()), table);
    }

    InplaceButton settingsButton = createSettingsButton(project, () -> cancel(popup[0]), showDialogAndFindUsagesRunnable);

    ActiveComponent statusComponent = new ActiveComponent() {
      @Override
      public void setActive(boolean active) {
        statusPanel.setActive(active);
      }

      @NotNull
      @Override
      public JComponent getComponent() {
        return statusPanel;
      }
    };
    DefaultActionGroup pinGroup = new DefaultActionGroup();
    ActiveComponent pin = createPinButton(project, popup, pinGroup, actionHandler::findUsages);
    builder.setCommandButton(new CompositeActiveComponent(statusComponent, settingsButton, pin));

    DefaultActionGroup toolbar = new DefaultActionGroup();
    usageView.addFilteringActions(toolbar);

    toolbar.add(UsageGroupingRuleProviderImpl.createGroupByFileStructureAction(usageView));
    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.USAGE_VIEW_TOOLBAR, toolbar, true);
    actionToolbar.setReservePlaceAutoPopupIcon(false);
    JComponent toolBar = actionToolbar.getComponent();
    toolBar.setOpaque(false);
    builder.setSettingButton(toolBar);
    builder.setCancelKeyEnabled(false);

    PopupUpdateProcessor processor = new PopupUpdateProcessor(usageView.getProject()) {
      @Override
      public void updatePopup(Object lookupItemObject) {/*not used*/}
    };
    builder.addListener(processor);

    popup[0] = builder.createPopup();
    JComponent content = popup[0].getContent();

    String fullTitle = title + getStatusString(true, false, 0, 0);
    int approxWidth = (int)(toolBar.getPreferredSize().getWidth()
                            + new JLabel(fullTitle).getPreferredSize().getWidth()
                            + settingsButton.getPreferredSize().getWidth());
    minWidth.set(Math.max(minWidth.get(), approxWidth));
    for (AnAction action : toolbar.getChildren(null)) {
      action.unregisterCustomShortcutSet(usageView.getComponent());
      action.registerCustomShortcutSet(action.getShortcutSet(), content);
    }

    for (AnAction action : pinGroup.getChildren(null)) {
      action.unregisterCustomShortcutSet(usageView.getComponent());
      action.registerCustomShortcutSet(action.getShortcutSet(), content);
    }

    return popup[0];
  }

  @NotNull
  private static ActiveComponent createPinButton(@NotNull Project project,
                                                 JBPopup @NotNull [] popup,
                                                 @NotNull DefaultActionGroup pinGroup,
                                                 @NotNull Runnable findUsagesRunnable) {
    Icon icon = ToolWindowManagerEx.getInstanceEx(project).getLocationIcon(ToolWindowId.FIND, AllIcons.General.Pin_tab);
    AnAction pinAction =
      new AnAction(IdeBundle.messagePointer("show.in.find.window.button.name"),
                   IdeBundle.messagePointer("show.in.find.window.button.pin.description"), icon) {
        {
          AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES);
          setShortcutSet(action.getShortcutSet());
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          hideHints();
          cancel(popup[0]);
          findUsagesRunnable.run();
        }
      };
    pinGroup.add(pinAction);
    ActionToolbar pinToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.USAGE_VIEW_TOOLBAR, pinGroup, true);
    pinToolbar.setReservePlaceAutoPopupIcon(false);
    JComponent pinToolBar = pinToolbar.getComponent();
    pinToolBar.setBorder(null);
    pinToolBar.setOpaque(false);

    return new ActiveComponent.Adapter() {
      @NotNull
      @Override
      public JComponent getComponent() {
        return pinToolBar;
      }
    };
  }

  private static void cancel(@Nullable JBPopup popup) {
    if (popup != null) {
      popup.cancel();
    }
  }

  private static @NotNull String getStatusString(boolean findUsagesInProgress, boolean hasMore, int visibleCount, int totalCount) {
    if (findUsagesInProgress || hasMore) {
      return UsageViewBundle.message("showing.0.usages", visibleCount - (hasMore ? 1 : 0));
    }
    else if (visibleCount != totalCount) {
      return UsageViewBundle.message("showing.0.of.1.usages", visibleCount, totalCount);
    }
    else {
      return UsageViewBundle.message("found.0.usages", totalCount);
    }
  }

  @NotNull
  private static String suggestSecondInvocation(@NotNull String text, @Nullable String title) {
    if (title != null) {
      text += "<br><small>" + title + "</small>";
    }
    return XmlStringUtil.wrapInHtml(UIUtil.convertSpace2Nbsp(text));
  }

  @Nullable
  static KeyboardShortcut getShowUsagesShortcut() {
    return ActionManager.getInstance().getKeyboardShortcut(ID);
  }

  private static int filtered(@NotNull List<? extends Usage> usages, @NotNull UsageViewImpl usageView) {
    return (int)usages.stream().filter(usage -> !usageView.isVisible(usage)).count();
  }

  private static int getUsageOffset(@NotNull Usage usage) {
    if (!(usage instanceof UsageInfo2UsageAdapter)) return -1;
    PsiElement element = ((UsageInfo2UsageAdapter)usage).getElement();
    if (element == null) return -1;
    return element.getTextRange().getStartOffset();
  }

  private static boolean areAllUsagesInOneLine(@NotNull Usage visibleUsage, @NotNull List<? extends Usage> usages) {
    Editor editor = getEditorFor(visibleUsage);
    if (editor == null) return false;
    int offset = getUsageOffset(visibleUsage);
    if (offset == -1) return false;
    int lineNumber = editor.getDocument().getLineNumber(offset);
    for (Usage other : usages) {
      Editor otherEditor = getEditorFor(other);
      if (otherEditor != editor) return false;
      int otherOffset = getUsageOffset(other);
      if (otherOffset == -1) return false;
      int otherLine = otherEditor.getDocument().getLineNumber(otherOffset);
      if (otherLine != lineNumber) return false;
    }
    return true;
  }

  private static int calcMaxWidth(@NotNull JTable table) {
    int colsNum = table.getColumnModel().getColumnCount();

    int totalWidth = 0;
    for (int col = 0; col < colsNum - 1; col++) {
      TableColumn column = table.getColumnModel().getColumn(col);
      int preferred = column.getPreferredWidth();
      int width = Math.max(preferred, columnMaxWidth(table, col));
      totalWidth += width;
      column.setMinWidth(width);
      column.setMaxWidth(width);
      column.setWidth(width);
      column.setPreferredWidth(width);
    }

    totalWidth += columnMaxWidth(table, colsNum - 1);

    return totalWidth;
  }

  private static int columnMaxWidth(@NotNull JTable table, int col) {
    TableColumn column = table.getColumnModel().getColumn(col);
    int width = 0;
    for (int row = 0; row < table.getRowCount(); row++) {
      Component component = table.prepareRenderer(column.getCellRenderer(), row, col);

      int rendererWidth = component.getPreferredSize().width;
      width = Math.max(width, rendererWidth + table.getIntercellSpacing().width);
    }
    return width;
  }

  private static void rebuildTable(@NotNull UsageViewImpl usageView,
                                   @NotNull List<UsageNode> data,
                                   @NotNull ShowUsagesTable table,
                                   @Nullable JBPopup popup,
                                   @NotNull RelativePoint popupPosition,
                                   @NotNull IntRef minWidth) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    ShowUsagesTable.MyModel tableModel = table.setTableModel(data);
    List<UsageNode> existingData = tableModel.getItems();

    int row = table.getSelectedRow();

    int newSelection = updateModel(tableModel, existingData, data, row == -1 ? 0 : row);
    if (newSelection < 0 || newSelection >= tableModel.getRowCount()) {
      ScrollingUtil.ensureSelectionExists(table);
      newSelection = table.getSelectedRow();
    }
    else {
      // do not pre-select the usage under caret by default
      if (newSelection == 0 && table.getModel().getRowCount() > 1) {
        Object valueInTopRow = table.getModel().getValueAt(0, 0);
        if (valueInTopRow instanceof UsageNode && usageView.isOriginUsage(((UsageNode)valueInTopRow).getUsage())) {
          newSelection++;
        }
      }
      table.getSelectionModel().setSelectionInterval(newSelection, newSelection);
    }
    ScrollingUtil.ensureIndexIsVisible(table, newSelection, 0);

    if (popup != null) {
      setSizeAndDimensions(table, popup, popupPosition, minWidth, data);
    }
  }

  // returns new selection
  private static int updateModel(@NotNull ShowUsagesTable.MyModel tableModel,
                                 @NotNull List<? extends UsageNode> listOld,
                                 @NotNull List<? extends UsageNode> listNew,
                                 int oldSelection) {
    UsageNode[] oa = listOld.toArray(new UsageNode[0]);
    UsageNode[] na = listNew.toArray(new UsageNode[0]);
    List<ModelDiff.Cmd> cmds = ModelDiff.createDiffCmds(tableModel, oa, na);
    int selection = oldSelection;
    if (cmds != null) {
      for (ModelDiff.Cmd cmd : cmds) {
        selection = cmd.translateSelection(selection);
        cmd.apply();
      }
    }
    return selection;
  }

  private static void setSizeAndDimensions(@NotNull JTable table,
                                           @NotNull JBPopup popup,
                                           @NotNull RelativePoint popupPosition,
                                           @NotNull IntRef minWidth,
                                           @NotNull List<? extends UsageNode> data) {
    JComponent content = popup.getContent();
    Window window = SwingUtilities.windowForComponent(content);
    Dimension d = window.getSize();

    int width = calcMaxWidth(table);
    width = (int)Math.max(d.getWidth(), width);
    Dimension headerSize = ((AbstractPopup)popup).getHeaderPreferredSize();
    width = Math.max((int)headerSize.getWidth(), width);
    width = Math.max(minWidth.get(), width);

    int delta = minWidth.get() == -1 ? 0 : width - minWidth.get();
    int newWidth = Math.max(width, d.width + delta);

    minWidth.set(newWidth);

    Dimension footerSize = ((AbstractPopup)popup).getFooterPreferredSize();

    int footer = footerSize.height;
    int footerBorder = footer == 0 ? 0 : 1;
    Insets insets = ((AbstractPopup)popup).getPopupBorder().getBorderInsets(content);
    int minHeight = headerSize.height + footer + footerBorder + insets.top + insets.bottom;

    Rectangle rectangle = getPreferredBounds(table, popupPosition.getScreenPoint(), newWidth, minHeight, data.size());
    table.setSize(rectangle.width, rectangle.height - minHeight);
    if (!data.isEmpty()) ScrollingUtil.ensureSelectionExists(table);

    Dimension newDim = rectangle.getSize();
    window.setBounds(rectangle);
    window.setMinimumSize(newDim);
    window.setMaximumSize(newDim);

    window.validate();
    window.repaint();
  }

  @NotNull
  private static Rectangle getPreferredBounds(@NotNull JTable table, @NotNull Point point, int width, int minHeight, int modelRows) {
    boolean addExtraSpace = Registry.is("ide.preferred.scrollable.viewport.extra.space");
    int visibleRows = Math.min(30, modelRows);
    int rowHeight = table.getRowHeight();
    int space = addExtraSpace && visibleRows < modelRows ? rowHeight / 2 : 0;
    int height = visibleRows * rowHeight + minHeight + space;
    Rectangle bounds = new Rectangle(point.x, point.y, width, height);
    ScreenUtil.fitToScreen(bounds);
    if (bounds.height != height) {
      minHeight += addExtraSpace && space == 0 ? rowHeight / 2 : space;
      bounds.height = Math.max(1, (bounds.height - minHeight) / rowHeight) * rowHeight + minHeight;
    }
    return bounds;
  }

  private static void addUsageNodes(@NotNull GroupNode root, @NotNull UsageViewImpl usageView, @NotNull List<? super UsageNode> outNodes) {
    for (UsageNode node : root.getUsageNodes()) {
      Usage usage = node.getUsage();
      if (usageView.isVisible(usage)) {
        node.setParent(root);
        outNodes.add(node);
      }
    }
    for (GroupNode groupNode : root.getSubGroups()) {
      groupNode.setParent(root);
      addUsageNodes(groupNode, usageView, outNodes);
    }
  }

  private static void navigateAndHint(@NotNull Project project,
                                      @NotNull Usage usage,
                                      @NotNull RelativePoint popupPosition,
                                      @NotNull String hint,
                                      @NotNull ShowUsagesActionHandler actionHandler) {
    usage.navigate(true);
    Editor newEditor = getEditorFor(usage);
    if (newEditor == null) return;
    hint(project, newEditor, popupPosition, false, hint, actionHandler);
  }

  private static void hint(@NotNull Project project,
                           @Nullable Editor editor,
                           @NotNull RelativePoint popupPosition,
                           boolean isWarning,
                           @NotNull String hint,
                           @NotNull ShowUsagesActionHandler actionHandler) {
    Runnable runnable = () -> {
      if (!actionHandler.isValid()) {
        return;
      }
      JComponent label = createHintComponent(
        suggestSecondInvocation(hint, getSecondInvocationTitle(actionHandler)),
        isWarning,
        createSettingsButton(
          project,
          ShowUsagesAction::hideHints,
          () -> actionHandler.showDialogAndShowUsages(editor)
        )
      );

      ShowUsagesActionState state = getState(project);
      state.continuation = () -> showUsagesInMaximalScope(actionHandler);
      runWhenHidden(label, () -> state.continuation = null);

      if (editor == null || editor.isDisposed() || !editor.getComponent().isShowing()) {
        int flags = HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING;
        HintManager.getInstance().showHint(label, popupPosition, flags, 0);
      }
      else {
        HintManager.getInstance().showInformationHint(editor, label);
      }
    };

    if (editor == null) {
      //opening editor is performing in invokeLater
      IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> {
        // after new editor created, some editor resizing events are still bubbling. To prevent hiding hint, invokeLater this
        IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(runnable);
      });
    }
    else {
      //opening editor is performing in invokeLater
      IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() ->
        editor.getScrollingModel().runActionOnScrollingFinished(() -> {
          // after new editor created, some editor resizing events are still bubbling. To prevent hiding hint, invokeLater this
          IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> AsyncEditorLoader.performWhenLoaded(editor, runnable));
        })
      );
    }
  }

  @Nullable
  private static Editor getEditorFor(@NotNull Usage usage) {
    FileEditorLocation location = usage.getLocation();
    FileEditor newFileEditor = location == null ? null : location.getEditor();
    return newFileEditor instanceof TextEditor ? ((TextEditor)newFileEditor).getEditor() : null;
  }

  static class StringNode extends UsageNode {
    @NotNull private final Object myString;

    private StringNode(@NotNull Object string) {
      super(null, NullUsage.INSTANCE);
      myString = string;
    }

    @Override
    public String toString() {
      return myString.toString();
    }
  }

  @Service
  private static final class ShowUsagesActionState {
    Runnable continuation;
  }

  @NotNull
  private static ShowUsagesActionState getState(@NotNull Project project) {
    return ServiceManager.getService(project, ShowUsagesActionState.class);
  }

  private static void runWhenHidden(@NotNull Component c, @NotNull Runnable r) {
    c.addHierarchyListener(runWhenHidden(r));
  }

  @NotNull
  private static HierarchyListener runWhenHidden(@NotNull Runnable r) {
    return new HierarchyListener() {
      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        if (!BitUtil.isSet(e.getChangeFlags(), HierarchyEvent.DISPLAYABILITY_CHANGED)) {
          return;
        }
        Component component = e.getComponent();
        if (component.isDisplayable()) {
          return;
        }
        r.run();
        component.removeHierarchyListener(this);
      }
    };
  }

  /**
   * @deprecated please use {@link #startFindUsages(PsiElement, RelativePoint, Editor)} overload
   */
  @Deprecated
  @ScheduledForRemoval(inVersion = "2020.3")
  public void startFindUsages(@NotNull PsiElement element,
                              @NotNull RelativePoint popupPosition,
                              @Nullable Editor editor,
                              @SuppressWarnings("unused") int maxUsages) {
    startFindUsages(element, popupPosition, editor);
  }
}
