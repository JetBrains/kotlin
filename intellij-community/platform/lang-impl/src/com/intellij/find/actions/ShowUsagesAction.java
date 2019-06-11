// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.FindManager;
import com.intellij.find.FindSettings;
import com.intellij.find.UsagesPreviewPanelProvider;
import com.intellij.find.findUsages.*;
import com.intellij.find.findUsages.FindUsagesHandlerFactory.OperationMode;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.gotoByName.ModelDiff;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.AsyncEditorLoader;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.preview.PreviewManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
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
import com.intellij.usages.*;
import com.intellij.usages.impl.*;
import com.intellij.usages.rules.UsageFilteringRuleProvider;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.*;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShowUsagesAction extends AnAction implements PopupAction {
  public static final String ID = "ShowUsages";

  public static int getUsagesPageSize() {
    return Math.max(1, Registry.intValue("ide.usages.page.size", 100));
  }

  private static final UsageNode MORE_USAGES_SEPARATOR_NODE = UsageViewImpl.NULL_NODE;
  private static final UsageNode USAGES_OUTSIDE_SCOPE_NODE = new UsageNode(null, ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR);

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
      //noinspection unchecked
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

  private Runnable mySearchEverywhereRunnable;

  public ShowUsagesAction() {
    setInjectedContext(true);
  }

  @Override
  public boolean startInTransaction() {
    return true;
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
    final Project project = e.getProject();
    if (project == null) return;

    Runnable searchEverywhere = mySearchEverywhereRunnable;
    mySearchEverywhereRunnable = null;
    hideHints();

    if (searchEverywhere != null) {
      searchEverywhere.run();
      return;
    }

    final RelativePoint popupPosition = JBPopupFactory.getInstance().guessBestPopupLocation(e.getDataContext());
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.usages");

    UsageTarget[] usageTargets = e.getData(UsageView.USAGE_TARGETS_KEY);
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (usageTargets == null) {
      FindUsagesAction.chooseAmbiguousTargetAndPerform(project, editor, element -> {
        startFindUsages(element, popupPosition, editor, getUsagesPageSize());
        return false;
      });
    }
    else if (ArrayUtil.getFirstElement(usageTargets) instanceof PsiElementUsageTarget) {
      PsiElement element = ((PsiElementUsageTarget)usageTargets[0]).getElement();
      if (element != null) {
        startFindUsages(element, popupPosition, editor, getUsagesPageSize());
      }
    }
  }

  private static void hideHints() {
    HintManager.getInstance().hideHints(HintManager.HIDE_BY_ANY_KEY, false, false);
  }

  public void startFindUsages(@NotNull PsiElement element, @NotNull RelativePoint popupPosition, Editor editor, int maxUsages) {
    Project project = element.getProject();
    FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
    FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, OperationMode.USAGES_WITH_DEFAULT_OPTIONS);
    if (handler == null) return;
    showElementUsages(editor, popupPosition, handler, maxUsages, handler.getFindUsagesOptions(DataManager.getInstance().getDataContext()), 0);
  }

  void showElementUsages(final Editor editor,
                         @NotNull final RelativePoint popupPosition,
                         @NotNull final FindUsagesHandler handler,
                         final int maxUsages,
                         @NotNull final FindUsagesOptions options, int minWidth) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final UsageViewSettings usageViewSettings = UsageViewSettings.getInstance();
    final ShowUsagesSettings showUsagesSettings = ShowUsagesSettings.getInstance();
    final UsageViewSettings savedGlobalSettings = new UsageViewSettings();

    savedGlobalSettings.loadState(usageViewSettings);
    usageViewSettings.loadState(showUsagesSettings.getState());

    final Project project = handler.getProject();
    UsageViewManager manager = UsageViewManager.getInstance(project);
    FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
    final UsageViewPresentation presentation = findUsagesManager.createPresentation(handler, options);
    presentation.setDetachedMode(true);
    UsageViewImpl usageView = (UsageViewImpl)manager.createUsageView(UsageTarget.EMPTY_ARRAY, Usage.EMPTY_ARRAY, presentation, null);
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

    final ShowUsagesTable table = new ShowUsagesTable();
    final AsyncProcessIcon processIcon = new AsyncProcessIcon("xxx");

    addUsageNodes(usageView.getRoot(), usageView, new ArrayList<>());

    final List<Usage> usages = new ArrayList<>();
    final Set<UsageNode> visibleNodes = new LinkedHashSet<>();
    final List<UsageNode> data = collectData(usages, visibleNodes, usageView, presentation);
    final AtomicInteger outOfScopeUsages = new AtomicInteger();
    table.setTableModel(usageView, data, outOfScopeUsages, options.searchScope);


    boolean isPreviewMode = Boolean.TRUE == PreviewManager.SERVICE.preview(handler.getProject(), UsagesPreviewPanelProvider.ID, Pair.create(usageView, table), false);
    Runnable itemChosenCallback = table.prepareTable(editor, popupPosition, handler, maxUsages, options, isPreviewMode, this);

    JBPopup popup = isPreviewMode ? null : createUsagePopup(usages, visibleNodes, handler, editor, popupPosition,
                                                            maxUsages, usageView, options, table, itemChosenCallback, presentation, processIcon, minWidth);
    if (popup != null) {
      Disposer.register(popup, usageView);

      // show popup only if find usages takes more than 300ms, otherwise it would flicker needlessly
      Alarm alarm = new Alarm(usageView);
      alarm.addRequest(() -> showPopupIfNeedTo(popup, popupPosition), 300);
    }

    final PingEDT pingEDT = new PingEDT("Rebuild popup in EDT", o -> popup != null && popup.isDisposed(), 100, () -> {
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

      rebuildTable(usageView, copy, nodes, table, popup, presentation, popupPosition, !processIcon.isDisposed(), outOfScopeUsages,
                   options.searchScope);
    });

    final MessageBusConnection messageBusConnection = project.getMessageBus().connect(usageView);
    messageBusConnection.subscribe(UsageFilteringRuleProvider.RULES_CHANGED, pingEDT::ping);

    final UsageTarget[] myUsageTarget = {new PsiElement2UsageTargetAdapter(handler.getPsiElement())};
    Processor<Usage> collect = usage -> {
      if (!UsageViewManagerImpl.isInScope(usage, options.searchScope)) {
        if (outOfScopeUsages.getAndIncrement() == 0) {
          visibleNodes.add(USAGES_OUTSIDE_SCOPE_NODE);
          usages.add(ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR);
        }
        return true;
      }
      synchronized (usages) {
        if (visibleNodes.size() >= maxUsages) return false;
        if(UsageViewManager.isSelfUsage(usage, myUsageTarget)) return true;
        UsageNode node = ReadAction.compute(() -> usageView.doAppendUsage(usage));
        usages.add(usage);
        if (node != null) {
          visibleNodes.add(node);
          boolean continueSearch = true;
          if (visibleNodes.size() == maxUsages) {
            visibleNodes.add(MORE_USAGES_SEPARATOR_NODE);
            usages.add(ShowUsagesTable.MORE_USAGES_SEPARATOR);
            continueSearch = false;
          }
          pingEDT.ping();

          return continueSearch;
        }
      }

      return true;
    };

    final ProgressIndicator indicator = FindUsagesManager.startProcessUsages(handler, handler.getPrimaryElements(), handler.getSecondaryElements(), collect,
       options, ()-> ApplicationManager.getApplication().invokeLater(() -> {
         Disposer.dispose(processIcon);
         Container parent = processIcon.getParent();
         if (parent != null) {
           parent.remove(processIcon);
           parent.repaint();
         }
         pingEDT.ping(); // repaint title
         synchronized (usages) {
           if (visibleNodes.isEmpty()) {
             if (usages.isEmpty()) {
               String text = UsageViewBundle.message("no.usages.found.in", searchScopePresentableName(options));
               hint(editor, text, handler, popupPosition, maxUsages, options, false);
               cancel(popup);
             }
             // else all usages filtered out
           }
           else if (visibleNodes.size() == 1) {
             if (usages.size() == 1) {
               //the only usage
               Usage usage = visibleNodes.iterator().next().getUsage();
               if (usage == ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR) {
                 hint(editor, UsageViewManagerImpl.outOfScopeMessage(outOfScopeUsages.get(), options.searchScope), handler, popupPosition, maxUsages, options, true);
               }
               else {
                 String message = UsageViewBundle.message("show.usages.only.usage", searchScopePresentableName(options));
                 navigateAndHint(usage, message, handler, popupPosition, maxUsages, options);
               }
               cancel(popup);
             }
             else {
               assert usages.size() > 1 : usages;
               // usage view can filter usages down to one
               Usage visibleUsage = visibleNodes.iterator().next().getUsage();
               if (areAllUsagesInOneLine(visibleUsage, usages)) {
                 String hint = UsageViewBundle.message("all.usages.are.in.this.line", usages.size(), searchScopePresentableName(options));
                 navigateAndHint(visibleUsage, hint, handler, popupPosition, maxUsages, options);
                 cancel(popup);
               }
             }
           }
           else {
             if (popup != null) {
               String title = presentation.getTabText();
               boolean shouldShowMoreSeparator = visibleNodes.contains(MORE_USAGES_SEPARATOR_NODE);
               String fullTitle = getFullTitle(usages, title, shouldShowMoreSeparator,
                                               visibleNodes.size() - (shouldShowMoreSeparator ? 1 : 0), false);
               popup.setCaption(fullTitle);
             }
           }
         }
       }, project.getDisposed()));
    if (popup != null) {
      Disposer.register(popup, indicator::cancel);
    }
  }

  @NotNull
  static UsageNode createStringNode(@NotNull final Object string) {
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
  private JComponent createHintComponent(@NotNull String text,
                                         @NotNull final FindUsagesHandler handler,
                                         @NotNull final RelativePoint popupPosition,
                                         final Editor editor,
                                         @NotNull final Runnable cancelAction,
                                         final int maxUsages,
                                         @NotNull final FindUsagesOptions options,
                                         boolean isWarning) {
    JComponent label = HintUtil.createInformationLabel(suggestSecondInvocation(options, handler, text + "&nbsp;"));
    if (isWarning) {
      label.setBackground(MessageType.WARNING.getPopupBackground());
    }
    InplaceButton button = createSettingsButton(handler, popupPosition, editor, maxUsages, cancelAction);

    JPanel panel = new JPanel(new BorderLayout()) {
      @Override
      public void addNotify() {
        mySearchEverywhereRunnable = () -> searchEverywhere(options, handler, editor, popupPosition, maxUsages);
        super.addNotify();
      }

      @Override
      public void removeNotify() {
        mySearchEverywhereRunnable = null;
        super.removeNotify();
      }
    };
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
  private InplaceButton createSettingsButton(@NotNull final FindUsagesHandler handler,
                                             @NotNull final RelativePoint popupPosition,
                                             final Editor editor,
                                             final int maxUsages,
                                             @NotNull final Runnable cancelAction) {
    String shortcutText = "";
    KeyboardShortcut shortcut = UsageViewImpl.getShowUsagesWithSettingsShortcut();
    if (shortcut != null) {
      shortcutText = "(" + KeymapUtil.getShortcutText(shortcut) + ")";
    }
    return new InplaceButton("Settings..." + shortcutText, AllIcons.General.Settings, e -> {
      int minWidth = myWidth;
      TransactionGuard.getInstance().submitTransactionLater(handler.getProject(), () -> showDialogAndFindUsages(handler, popupPosition, editor, maxUsages, minWidth));
      cancelAction.run();
    });
  }

  private void showDialogAndFindUsages(@NotNull FindUsagesHandler handler,
                                       @NotNull RelativePoint popupPosition,
                                       Editor editor,
                                       int maxUsages, int minWidth) {
    AbstractFindUsagesDialog dialog = handler.getFindUsagesDialog(false, false, false);
    if (dialog.showAndGet()) {
      dialog.calcFindUsagesOptions();
      FindUsagesOptions options = handler.getFindUsagesOptions(DataManager.getInstance().getDataContext());
      showElementUsages(editor, popupPosition, handler, maxUsages, options, minWidth);
    }
  }

  @NotNull
  private static String searchScopePresentableName(@NotNull FindUsagesOptions options) {
    return options.searchScope.getDisplayName();
  }

  @NotNull
  private JBPopup createUsagePopup(@NotNull final List<Usage> usages,
                                   @NotNull Set<? extends UsageNode> visibleNodes,
                                   @NotNull final FindUsagesHandler handler,
                                   final Editor editor,
                                   @NotNull final RelativePoint popupPosition,
                                   final int maxUsages,
                                   @NotNull final UsageViewImpl usageView,
                                   @NotNull final FindUsagesOptions options,
                                   @NotNull final JTable table,
                                   @NotNull final Runnable itemChoseCallback,
                                   @NotNull final UsageViewPresentation presentation,
                                   @NotNull final AsyncProcessIcon processIcon, int minWidth) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    PopupChooserBuilder builder = JBPopupFactory.getInstance().createPopupChooserBuilder(table);
    final String title = presentation.getTabText();
    String fullTitle;
    if (title == null) {
      fullTitle = "";
    }
    else {
      fullTitle = getFullTitle(usages, title, false, visibleNodes.size() - 1, true);
      builder.setTitle(fullTitle);
      builder.setAdText(getSecondInvocationTitle(options, handler));
    }

    builder.setMovable(true).setResizable(true);
    builder.setItemChoosenCallback(itemChoseCallback);
    final JBPopup[] popup = new JBPopup[1];

    KeyboardShortcut shortcut = UsageViewImpl.getShowUsagesWithSettingsShortcut();
    if (shortcut != null) {
      new DumbAwareAction() {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          cancel(popup[0]);
          showDialogAndFindUsages(handler, popupPosition, editor, maxUsages, myWidth);
        }

        @Override
        public boolean startInTransaction() {
          return true;
        }
      }.registerCustomShortcutSet(new CustomShortcutSet(shortcut.getFirstKeyStroke()), table);
    }
    shortcut = getShowUsagesShortcut();
    if (shortcut != null) {
      new DumbAwareAction() {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          cancel(popup[0]);
          searchEverywhere(options, handler, editor, popupPosition, maxUsages);
        }

        @Override
        public boolean startInTransaction() {
          return true;
        }
      }.registerCustomShortcutSet(new CustomShortcutSet(shortcut.getFirstKeyStroke()), table);
    }

    InplaceButton settingsButton = createSettingsButton(handler, popupPosition, editor, maxUsages, () -> cancel(popup[0]));

    ActiveComponent spinningProgress = new ActiveComponent.Adapter() {
      @NotNull
      @Override
      public JComponent getComponent() {
        return processIcon;
      }
    };
    final DefaultActionGroup pinGroup = new DefaultActionGroup();
    final ActiveComponent pin = createPinButton(handler, usageView, options, popup, pinGroup);
    builder.setCommandButton(new CompositeActiveComponent(spinningProgress, settingsButton, pin));

    DefaultActionGroup toolbar = new DefaultActionGroup();
    usageView.addFilteringActions(toolbar);

    toolbar.add(UsageGroupingRuleProviderImpl.createGroupByFileStructureAction(usageView)); 
    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.USAGE_VIEW_TOOLBAR, toolbar, true);
    actionToolbar.setReservePlaceAutoPopupIcon(false);
    final JComponent toolBar = actionToolbar.getComponent();
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

    int approxWidth = (int)(toolBar.getPreferredSize().getWidth()
                            + new JLabel(fullTitle).getPreferredSize().getWidth()
                            + settingsButton.getPreferredSize().getWidth());
    myWidth = Math.max(minWidth, approxWidth);
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
  private ActiveComponent createPinButton(@NotNull final FindUsagesHandler handler,
                                          @NotNull final UsageViewImpl usageView,
                                          @NotNull final FindUsagesOptions options,
                                          @NotNull final JBPopup[] popup,
                                          @NotNull DefaultActionGroup pinGroup) {
    Icon icon = ToolWindowManagerEx.getInstanceEx(handler.getProject()).getLocationIcon(ToolWindowId.FIND, AllIcons.General.Pin_tab);
    final AnAction pinAction =
      new AnAction("Open Find Usages Toolwindow", "Show all usages in a separate toolwindow", icon) {
        {
          AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES);
          setShortcutSet(action.getShortcutSet());
        }

        @Override
        public boolean startInTransaction() {
          return true;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          hideHints();
          cancel(popup[0]);
          FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(usageView.getProject())).getFindUsagesManager();
          findUsagesManager.findUsages(handler.getPrimaryElements(), handler.getSecondaryElements(), handler, options,
                                       FindSettings.getInstance().isSkipResultsWithOneUsage());
        }
      };
    pinGroup.add(pinAction);
    final ActionToolbar pinToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.USAGE_VIEW_TOOLBAR, pinGroup, true);
    pinToolbar.setReservePlaceAutoPopupIcon(false);
    final JComponent pinToolBar = pinToolbar.getComponent();
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

  @NotNull
  private static String getFullTitle(@NotNull List<Usage> usages,
                                     @NotNull String title,
                                     boolean hadMoreSeparator,
                                     int visibleNodesCount,
                                     boolean findUsagesInProgress) {
    String soFarSuffix = findUsagesInProgress ? " so far" : "";
    title = StringUtil.escapeXmlEntities(title);
    String s;
    if (hadMoreSeparator) {
      s = "<b>Some</b> " + title + " " + "<b>(Only " + visibleNodesCount + " usages shown" + soFarSuffix + ")</b>";
    }
    else {
      s = title + " (" + UsageViewBundle.message("usages.n", usages.size()) + soFarSuffix + ")";
    }
    return XmlStringUtil.wrapInHtml("<body><nobr>" + s + "</nobr></body>");
  }

  @NotNull
  private static String suggestSecondInvocation(@NotNull FindUsagesOptions options, @NotNull FindUsagesHandler handler, @NotNull String text) {
    final String title = getSecondInvocationTitle(options, handler);

    if (title != null) {
        text += "<br><small> " + title + "</small>";
    }
    return XmlStringUtil.wrapInHtml(UIUtil.convertSpace2Nbsp(text));
  }

  @Nullable
  private static String getSecondInvocationTitle(@NotNull FindUsagesOptions options, @NotNull FindUsagesHandler handler) {
    if (getShowUsagesShortcut() != null) {
       GlobalSearchScope maximalScope = FindUsagesManager.getMaximalScope(handler);
      if (!options.searchScope.equals(maximalScope)) {
         return "Press " + KeymapUtil.getShortcutText(getShowUsagesShortcut()) + " again to search in " + maximalScope.getDisplayName();
       }
     }
     return null;
  }

  private void searchEverywhere(@NotNull FindUsagesOptions options,
                                @NotNull FindUsagesHandler handler,
                                Editor editor,
                                @NotNull RelativePoint popupPosition,
                                int maxUsages) {
    FindUsagesOptions cloned = options.clone();
    cloned.searchScope = FindUsagesManager.getMaximalScope(handler);
    showElementUsages(editor, popupPosition, handler, maxUsages, cloned, myWidth);
  }

  @Nullable
  private static KeyboardShortcut getShowUsagesShortcut() {
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

  @NotNull
  private static List<UsageNode> collectData(@NotNull List<? extends Usage> usages,
                                             @NotNull Collection<? extends UsageNode> visibleNodes,
                                             @NotNull UsageViewImpl usageView,
                                             @NotNull UsageViewPresentation presentation) {
    @NotNull List<UsageNode> data = new ArrayList<>();
    int filtered = filtered(usages, usageView);
    if (filtered != 0) {
      data.add(createStringNode(UsageViewBundle.message("usages.were.filtered.out", filtered)));
    }
    data.addAll(visibleNodes);
    if (data.isEmpty()) {
      String progressText = StringUtil.escapeXmlEntities(UsageViewManagerImpl.getProgressTitle(presentation));
      data.add(createStringNode(progressText));
    }
    Collections.sort(data, USAGE_NODE_COMPARATOR);
    return data;
  }

  private static int calcMaxWidth(@NotNull JTable table) {
    int colsNum = table.getColumnModel().getColumnCount();

    int totalWidth = 0;
    for (int col = 0; col < colsNum -1; col++) {
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

  int myWidth;

  private void rebuildTable(@NotNull final UsageViewImpl usageView,
                            @NotNull final List<Usage> usages,
                            @NotNull List<UsageNode> nodes,
                            @NotNull final ShowUsagesTable table,
                            @Nullable final JBPopup popup,
                            @NotNull final UsageViewPresentation presentation,
                            @NotNull final RelativePoint popupPosition,
                            boolean findUsagesInProgress,
                            @NotNull AtomicInteger outOfScopeUsages,
                            @NotNull SearchScope searchScope) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    boolean shouldShowMoreSeparator = usages.contains(ShowUsagesTable.MORE_USAGES_SEPARATOR);
    if (shouldShowMoreSeparator) {
      nodes.add(MORE_USAGES_SEPARATOR_NODE);
    }
    boolean hasOutsideScopeUsages = usages.contains(ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR);
    if (hasOutsideScopeUsages && !shouldShowMoreSeparator) {
      nodes.add(USAGES_OUTSIDE_SCOPE_NODE);
    }

    String title = presentation.getTabText();
    String fullTitle = getFullTitle(usages, title, shouldShowMoreSeparator || hasOutsideScopeUsages, nodes.size() - (shouldShowMoreSeparator || hasOutsideScopeUsages ? 1 : 0), findUsagesInProgress);
    if (popup != null) {
      popup.setCaption(fullTitle);
    }

    List<UsageNode> data = collectData(usages, nodes, usageView, presentation);
    ShowUsagesTable.MyModel tableModel = table.setTableModel(usageView, data, outOfScopeUsages, searchScope);
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
      setSizeAndDimensions(table, popup, popupPosition, data);
    }
  }

  // returns new selection
  private static int updateModel(@NotNull ShowUsagesTable.MyModel tableModel, @NotNull List<UsageNode> listOld, @NotNull List<UsageNode> listNew, int oldSelection) {
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

  private void setSizeAndDimensions(@NotNull JTable table,
                                    @NotNull JBPopup popup,
                                    @NotNull RelativePoint popupPosition,
                                    @NotNull List<UsageNode> data) {
    JComponent content = popup.getContent();
    Window window = SwingUtilities.windowForComponent(content);
    Dimension d = window.getSize();

    int width = calcMaxWidth(table);
    width = (int)Math.max(d.getWidth(), width);
    Dimension headerSize = ((AbstractPopup)popup).getHeaderPreferredSize();
    width = Math.max((int)headerSize.getWidth(), width);
    width = Math.max(myWidth, width);

    int delta = myWidth == -1 ? 0 : width - myWidth;
    int newWidth = Math.max(width, d.width + delta);

    myWidth = newWidth;

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

  void appendMoreUsages(Editor editor,
                        @NotNull RelativePoint popupPosition,
                        @NotNull FindUsagesHandler handler,
                        int maxUsages,
                        @NotNull FindUsagesOptions options) {
    int minWidth = myWidth;
    TransactionGuard.submitTransaction(handler.getProject(), () ->
      showElementUsages(editor, popupPosition, handler, maxUsages + getUsagesPageSize(), options, minWidth));
  }

  private static void addUsageNodes(@NotNull GroupNode root, @NotNull final UsageViewImpl usageView, @NotNull List<? super UsageNode> outNodes) {
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

  private void navigateAndHint(@NotNull Usage usage,
                               @Nullable final String hint,
                               @NotNull final FindUsagesHandler handler,
                               @NotNull final RelativePoint popupPosition,
                               final int maxUsages,
                               @NotNull final FindUsagesOptions options) {
    usage.navigate(true);
    if (hint == null) return;
    final Editor newEditor = getEditorFor(usage);
    if (newEditor == null) return;
    hint(newEditor, hint, handler, popupPosition, maxUsages, options, false);
  }

  private void showHint(@Nullable final Editor editor,
                        @NotNull String hint,
                        @NotNull FindUsagesHandler handler,
                        @NotNull final RelativePoint popupPosition,
                        int maxUsages,
                        @NotNull FindUsagesOptions options,
                        boolean isWarning) {
    Runnable runnable = () -> {
      if (!handler.getPsiElement().isValid()) return;

      JComponent label = createHintComponent(hint, handler, popupPosition, editor, ShowUsagesAction::hideHints, maxUsages, options, isWarning);
      if (editor == null || editor.isDisposed() || !editor.getComponent().isShowing()) {
        HintManager.getInstance().showHint(label, popupPosition, HintManager.HIDE_BY_ANY_KEY |
                                                                 HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0);
      }
      else {
        HintManager.getInstance().showInformationHint(editor, label);
      }
    };
    if (editor == null) {
      runnable.run();
    } else {
      AsyncEditorLoader.performWhenLoaded(editor, runnable);
    }
  }

  private void hint(@Nullable final Editor editor,
                    @NotNull final String hint,
                    @NotNull final FindUsagesHandler handler,
                    @NotNull final RelativePoint popupPosition,
                    final int maxUsages,
                    @NotNull final FindUsagesOptions options,
                    final boolean isWarning) {
    final Project project = handler.getProject();
    //opening editor is performing in invokeLater
    IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> {
      Runnable runnable = () -> {
        // after new editor created, some editor resizing events are still bubbling. To prevent hiding hint, invokeLater this
        IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(
          () -> showHint(editor, hint, handler, popupPosition, maxUsages, options, isWarning));
      };
      if (editor == null) {
        runnable.run();
      }
      else {
        editor.getScrollingModel().runActionOnScrollingFinished(runnable);
      }
    });
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
}
