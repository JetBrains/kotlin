// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.BigPopupUI;
import com.intellij.ide.actions.bigPopup.ShowFilterAction;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGeneralGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingRecentGroup;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.ide.actions.runAnything.ui.RunAnythingScrollingUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

import static com.intellij.ide.actions.runAnything.RunAnythingAction.ALT_IS_PRESSED;
import static com.intellij.ide.actions.runAnything.RunAnythingAction.SHIFT_IS_PRESSED;
import static com.intellij.ide.actions.runAnything.RunAnythingIconHandler.MATCHED_PROVIDER_PROPERTY;
import static com.intellij.openapi.wm.IdeFocusManager.getGlobalInstance;

public class RunAnythingPopupUI extends BigPopupUI {
  public static final int SEARCH_FIELD_COLUMNS = 25;
  public static final Icon UNKNOWN_CONFIGURATION_ICON = AllIcons.Actions.Run_anything;
  public static final DataKey<Executor> EXECUTOR_KEY = DataKey.create("EXECUTOR_KEY");
  static final String RUN_ANYTHING = "RunAnything";
  public static final KeyStroke DOWN_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
  public static final KeyStroke UP_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);

  private static final Logger LOG = Logger.getInstance(RunAnythingPopupUI.class);
  private static final Border RENDERER_BORDER = JBUI.Borders.empty(1, 0);
  private static final String HELP_PLACEHOLDER = "?";
  private static final int LIST_REBUILD_DELAY = 100;
  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, ApplicationManager.getApplication());
  private final AnActionEvent myActionEvent;
  private boolean myIsUsedTrigger;
  private CalcThread myCalcThread;
  private volatile ActionCallback myCurrentWorker;
  private int myCalcThreadRestartRequestId = 0;
  private final Object myWorkerRestartRequestLock = new Object();
  private boolean mySkipFocusGain = false;
  @Nullable
  private VirtualFile myVirtualFile;
  @NotNull private final DataContext myDataContext;
  private JLabel myTextFieldTitle;
  private boolean myIsItemSelected;
  private String myLastInputText = null;
  private RunAnythingSearchListModel.RunAnythingMainListModel myListModel;
  private Project myProject;
  private Module myModule;

  private void onMouseClicked(@NotNull MouseEvent event) {
    int clickCount = event.getClickCount();
    if (clickCount > 1 && clickCount % 2 == 0) {
      event.consume();
      final int i = myResultsList.locationToIndex(event.getPoint());
      if (i != -1) {
        getGlobalInstance().doWhenFocusSettlesDown(() -> getGlobalInstance().requestFocus(getField(), true));
        ApplicationManager.getApplication().invokeLater(() -> {
          myResultsList.setSelectedIndex(i);
          executeCommand();
        });
      }
    }
  }

  private void initSearchField() {
    mySearchField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        myIsUsedTrigger = true;

        final String pattern = mySearchField.getText();
        if (mySearchField.hasFocus()) {
          ApplicationManager.getApplication().invokeLater(() -> myIsItemSelected = false);

          if (!myIsItemSelected) {
            myLastInputText = null;
            clearSelection();

            rebuildList();
          }

          if (!isHelpMode(pattern)) {
            adjustMainListEmptyText(mySearchField);
            return;
          }

          adjustEmptyText(mySearchField, field -> true, "",
                          IdeBundle.message("run.anything.help.list.empty.secondary.text"));
        }
      }
    });
    mySearchField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (mySkipFocusGain) {
          mySkipFocusGain = false;
          return;
        }
        mySearchField.setForeground(UIUtil.getLabelForeground());
        mySearchField.setColumns(SEARCH_FIELD_COLUMNS);
        ApplicationManager.getApplication().invokeLater(() -> {
          final JComponent parent = (JComponent)mySearchField.getParent();
          parent.revalidate();
          parent.repaint();
        });
        rebuildList();
      }

      @Override
      public void focusLost(FocusEvent e) {
        final ActionCallback result = new ActionCallback();
        UIUtil.invokeLaterIfNeeded(() -> {
          try {
            if (myCalcThread != null) {
              myCalcThread.cancel();
            }
            myAlarm.cancelAllRequests();

            ApplicationManager.getApplication().invokeLater(() -> ActionToolbarImpl.updateAllToolbarsImmediately());

            searchFinishedHandler.run();
          }
          finally {
            result.setDone();
          }
        });
      }
    });
  }

  private static void adjustMainListEmptyText(@NotNull JBTextField editor) {
    adjustEmptyText(editor, field -> field.getText().isEmpty(), IdeBundle.message("run.anything.main.list.empty.primary.text"),
                    IdeBundle.message("run.anything.main.list.empty.secondary.text"));
  }

  private static boolean isHelpMode(@NotNull String pattern) {
    return pattern.startsWith(HELP_PLACEHOLDER);
  }

  private void clearSelection() {
    myResultsList.getSelectionModel().clearSelection();
  }

  private JTextField getField() {
    return mySearchField;
  }

  private void executeCommand() {
    final String pattern = getField().getText();
    int index = myResultsList.getSelectedIndex();

    //do nothing on attempt to execute empty command
    if (pattern.isEmpty() && index == -1) return;

    final Project project = getProject();

    final RunAnythingSearchListModel model = getSearchingModel(myResultsList);
    if (index != -1 && model != null && isMoreItem(index)) {
      RunAnythingGroup group = model.findGroupByMoreIndex(index);

      if (group != null) {
        myCurrentWorker.doWhenProcessed(() -> {
          myCalcThread = new CalcThread(project, pattern, true);
          RunAnythingUsageCollector.Companion.triggerMoreStatistics(project, group, model.getClass());
          myCurrentWorker = myCalcThread.insert(index, group);
        });

        return;
      }
    }

    if (model != null) {
      RunAnythingUsageCollector.Companion.triggerExecCategoryStatistics(project, model.getGroups(), model.getClass(), index,
                                                                        SHIFT_IS_PRESSED.get(), ALT_IS_PRESSED.get());
    }
    DataContext dataContext = createDataContext(myDataContext, ALT_IS_PRESSED.get());
    RunAnythingUtil.executeMatched(dataContext, pattern);

    searchFinishedHandler.run();
    triggerUsed();
  }

  @NotNull
  private DataContext createDataContext(@NotNull DataContext parentDataContext, boolean isAltPressed) {
    Map<String, Object> map = new HashMap<>();
    map.put(CommonDataKeys.VIRTUAL_FILE.getName(), getWorkDirectory(getModule(), isAltPressed));
    map.put(EXECUTOR_KEY.getName(), getExecutor());

    return SimpleDataContext.getSimpleContext(map, parentDataContext);
  }

  @NotNull
  private Project getProject() {
    return myProject;
  }

  @Nullable
  private Module getModule() {
    if (myModule != null) {
      return myModule;
    }

    Project project = getProject();
    if (myVirtualFile != null) {
      Module moduleForFile = ModuleUtilCore.findModuleForFile(myVirtualFile, project);
      if (moduleForFile != null) {
        return moduleForFile;
      }
    }

    VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
    if (selectedFiles.length != 0) {
      Module moduleForFile = ModuleUtilCore.findModuleForFile(selectedFiles[0], project);
      if (moduleForFile != null) {
        return moduleForFile;
      }
    }

    return null;
  }

  @NotNull
  private VirtualFile getWorkDirectory(@Nullable Module module, boolean isAltPressed) {
    if (isAltPressed) {
      if (myVirtualFile != null) {
        VirtualFile file = myVirtualFile.isDirectory() ? myVirtualFile : myVirtualFile.getParent();
        if (file != null) {
          return file;
        }
      }

      VirtualFile[] selectedFiles = FileEditorManager.getInstance(getProject()).getSelectedFiles();
      if (selectedFiles.length > 0) {
        VirtualFile file = selectedFiles[0].getParent();
        if (file != null) {
          return file;
        }
      }
    }

    return getBaseDirectory(module);
  }

  @NotNull
  private VirtualFile getBaseDirectory(@Nullable Module module) {
    VirtualFile projectBaseDir = getProject().getBaseDir();
    if (module == null) {
      return projectBaseDir;
    }

    VirtualFile firstContentRoot = getFirstContentRoot(module);
    if (firstContentRoot == null) {
      return projectBaseDir;
    }

    return firstContentRoot;
  }

  @Nullable
  public VirtualFile getFirstContentRoot(@NotNull final Module module) {
    if (module.isDisposed()) return null;
    return ArrayUtil.getFirstElement(ModuleRootManager.getInstance(module).getContentRoots());
  }

  private boolean isMoreItem(int index) {
    RunAnythingSearchListModel model = getSearchingModel(myResultsList);
    return model != null && model.isMoreIndex(index);
  }

  @Nullable
  public static RunAnythingSearchListModel getSearchingModel(@NotNull JBList list) {
    ListModel model = list.getModel();
    return model instanceof RunAnythingSearchListModel ? (RunAnythingSearchListModel)model : null;
  }

  private void rebuildList() {
    String pattern = getSearchPattern();
    assert EventQueue.isDispatchThread() : "Must be EDT";
    if (myCalcThread != null && !myCurrentWorker.isProcessed()) {
      myCurrentWorker = myCalcThread.cancel();
    }
    if (myCalcThread != null && !myCalcThread.isCanceled()) {
      myCalcThread.cancel();
    }
    synchronized (myWorkerRestartRequestLock) { // this lock together with RestartRequestId should be enough to prevent two CalcThreads running at the same time
      final int currentRestartRequest = ++myCalcThreadRestartRequestId;
      myCurrentWorker.doWhenProcessed(() -> {
        synchronized (myWorkerRestartRequestLock) {
          if (currentRestartRequest != myCalcThreadRestartRequestId) {
            return;
          }
          myCalcThread = new CalcThread(getProject(), pattern, false);
          myCurrentWorker = myCalcThread.start();
        }
      });
    }
  }

  public void initResultsList() {
    myResultsList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        updateAdText(myDataContext);

        Object selectedValue = myResultsList.getSelectedValue();
        if (selectedValue == null) return;

        String lastInput = mySearchField.getText();
        myIsItemSelected = true;

        if (isMoreItem(myResultsList.getSelectedIndex())) {
          if (myLastInputText != null) {
            mySearchField.setText(myLastInputText);
          }
          return;
        }

        mySearchField.setText(selectedValue instanceof RunAnythingItem ? ((RunAnythingItem)selectedValue).getCommand() : myLastInputText);

        if (myLastInputText == null) myLastInputText = lastInput;
      }
    });
  }

  @Override
  @NotNull
  public JPanel createTopLeftPanel() {
    myTextFieldTitle = new JLabel(IdeBundle.message("run.anything.run.anything.title"));
    JPanel topPanel = new NonOpaquePanel(new BorderLayout());
    Color foregroundColor = UIUtil.isUnderDarcula()
                            ? UIUtil.isUnderWin10LookAndFeel() ? JBColor.WHITE : new JBColor(Gray._240, Gray._200)
                            : UIUtil.getLabelForeground();


    myTextFieldTitle.setForeground(foregroundColor);
    myTextFieldTitle.setBorder(BorderFactory.createEmptyBorder(3, 5, 5, 0));
    if (SystemInfo.isMac) {
      myTextFieldTitle.setFont(myTextFieldTitle.getFont().deriveFont(Font.BOLD, myTextFieldTitle.getFont().getSize() - 1f));
    }
    else {
      myTextFieldTitle.setFont(myTextFieldTitle.getFont().deriveFont(Font.BOLD));
    }

    topPanel.add(myTextFieldTitle);

    return topPanel;
  }

  @NotNull
  public DataContext createDataContext(@NotNull AnActionEvent e) {
    HashMap<String, Object> dataMap = new HashMap<>();
    dataMap.put(CommonDataKeys.PROJECT.getName(), e.getProject());
    dataMap.put(LangDataKeys.MODULE.getName(), getModule());
    return createDataContext(SimpleDataContext.getSimpleContext(dataMap, e.getDataContext()), ALT_IS_PRESSED.get());
  }

  public void initMySearchField() {
    mySearchField.putClientProperty(MATCHED_PROVIDER_PROPERTY, UNKNOWN_CONFIGURATION_ICON);

    setHandleMatchedConfiguration();

    adjustMainListEmptyText(mySearchField);

    mySearchField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        updateByModifierKeysEvent(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        updateByModifierKeysEvent(e);
      }

      private void updateByModifierKeysEvent(@NotNull KeyEvent e) {
        String message;
        if (e.isShiftDown() && e.isAltDown()) {
          message = IdeBundle.message("run.anything.run.in.context.debug.title");
        }
        else if (e.isShiftDown()) {
          message = IdeBundle.message("run.anything.run.debug.title");
        }
        else if (e.isAltDown()) {
          message = IdeBundle.message("run.anything.run.in.context.title");
        }
        else {
          message = IdeBundle.message("run.anything.run.anything.title");
        }
        myTextFieldTitle.setText(message);
        updateMatchedRunConfigurationStuff(e.isAltDown());
      }
    });

    initSearchField();

    mySearchField.setColumns(SEARCH_FIELD_COLUMNS);
  }

  public static void adjustEmptyText(@NotNull JBTextField textEditor,
                                     @NotNull BooleanFunction<JBTextField> function,
                                     @NotNull String leftText,
                                     @NotNull String rightText) {

    textEditor.putClientProperty("StatusVisibleFunction", function);
    StatusText statusText = textEditor.getEmptyText();
    statusText.setIsVerticalFlow(false);
    statusText.setShowAboveCenter(false);
    statusText.setText(leftText, SimpleTextAttributes.GRAY_ATTRIBUTES);
    statusText.appendSecondaryText(rightText, SimpleTextAttributes.GRAY_ATTRIBUTES, null);
    statusText.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
  }

  private void setHandleMatchedConfiguration() {
    mySearchField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        updateMatchedRunConfigurationStuff(ALT_IS_PRESSED.get());
      }
    });
  }

  private void updateMatchedRunConfigurationStuff(boolean isAltPressed) {
    JBTextField textField = mySearchField;
    String pattern = textField.getText();

    DataContext dataContext = createDataContext(myDataContext, isAltPressed);
    RunAnythingProvider provider = RunAnythingProvider.findMatchedProvider(dataContext, pattern);

    if (provider == null) {
      return;
    }

    Object value = provider.findMatchingValue(dataContext, pattern);

    if (value == null) {
      return;
    }
    //noinspection unchecked
    Icon icon = provider.getIcon(value);
    if (icon == null) {
      return;
    }

    textField.putClientProperty(MATCHED_PROVIDER_PROPERTY, icon);
  }

  private void updateAdText(@NotNull DataContext dataContext) {
    Object value = myResultsList.getSelectedValue();

    if (value instanceof RunAnythingItem) {
      RunAnythingProvider provider = RunAnythingProvider.findMatchedProvider(dataContext, ((RunAnythingItem)value).getCommand());
      if (provider != null) {
        String adText = provider.getAdText();
        if (adText != null) {
          setAdText(adText);
        }
      }
    }
  }

  private void triggerUsed() {
    if (myIsUsedTrigger) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(RUN_ANYTHING);
    }
    myIsUsedTrigger = false;
  }


  public void setAdText(@NotNull final String s) {
    myHintLabel.setText(s);
  }

  @NotNull
  public static Executor getExecutor() {
    final Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();
    final Executor debugExecutor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);

    return !SHIFT_IS_PRESSED.get() ? runExecutor : debugExecutor;
  }

  private class MyListRenderer extends ColoredListCellRenderer<Object> {
    private final RunAnythingMyAccessibleComponent myMainPanel = new RunAnythingMyAccessibleComponent(new BorderLayout());

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
      Component cmp = null;
      if (isMoreItem(index)) {
        cmp = RunAnythingMore.get(isSelected);
      }

      if (cmp == null) {
        if (value instanceof RunAnythingItem) {
          cmp = ((RunAnythingItem)value).createComponent(myLastInputText, findIcon(index), isSelected, hasFocus);
        }
        else {
          cmp = super.getListCellRendererComponent(list, value, index, isSelected, isSelected);
          final JPanel p = new JPanel(new BorderLayout());
          p.setBackground(UIUtil.getListBackground(isSelected, true));
          p.add(cmp, BorderLayout.CENTER);
          cmp = p;
        }
      }

      Color bg = cmp.getBackground();
      if (bg == null) {
        cmp.setBackground(UIUtil.getListBackground(isSelected));
        bg = cmp.getBackground();
      }

      Color foreground = cmp.getForeground();
      if (foreground == null) {
        cmp.setForeground(UIUtil.getListForeground(isSelected));
        foreground = cmp.getBackground();
      }
      myMainPanel.removeAll();
      RunAnythingSearchListModel model = getSearchingModel(myResultsList);
      if (model != null) {
        String title = model.getTitle(index);
        if (title != null) {
          myMainPanel.add(RunAnythingUtil.createTitle(" " + title), BorderLayout.NORTH);
        }
      }
      JPanel wrapped = new JPanel(new BorderLayout());
      wrapped.setBackground(bg);
      wrapped.setForeground(foreground);
      wrapped.setBorder(RENDERER_BORDER);
      wrapped.add(cmp, BorderLayout.CENTER);
      myMainPanel.add(wrapped, BorderLayout.CENTER);
      if (cmp instanceof Accessible) {
        myMainPanel.setAccessible((Accessible)cmp);
      }

      return myMainPanel;
    }

    @Nullable
    private Icon findIcon(int index) {
      RunAnythingSearchListModel model = getSearchingModel(myResultsList);
      Icon groupIcon = null;
      if (model != null) {
        RunAnythingGroup group = model.findItemGroup(index);
        if (group != null) {
          groupIcon = group.getIcon();
        }
      }
      return groupIcon;
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList list, final Object value, int index, final boolean selected, boolean hasFocus) {
    }
  }

  private class CalcThread implements Runnable {
    @NotNull private final Project myProject;
    @NotNull private final String myPattern;
    private final ProgressIndicator myProgressIndicator = new ProgressIndicatorBase();
    private final ActionCallback myDone = new ActionCallback();
    @NotNull private final RunAnythingSearchListModel myListModel;

    private CalcThread(@NotNull Project project, @NotNull String pattern, boolean reuseModel) {
      myProject = project;
      myPattern = pattern;
      RunAnythingSearchListModel model = getSearchingModel(myResultsList);

      myListModel = reuseModel && model != null
                    ? model
                    : isHelpMode(pattern)
                      ? new RunAnythingSearchListModel.RunAnythingHelpListModel()
                      : new RunAnythingSearchListModel.RunAnythingMainListModel();
    }

    @Override
    public void run() {
      try {
        check();

        ApplicationManager.getApplication().invokeLater(() -> {
          // this line must be called on EDT to avoid context switch at clear().append("text") Don't touch. Ask [kb]
          myResultsList.getEmptyText().setText("Searching...");

          if (getSearchingModel(myResultsList) != null) {
            myAlarm.cancelAllRequests();
            myAlarm.addRequest(() -> {
              if (DumbService.getInstance(myProject).isDumb()) {
                myResultsList.setEmptyText(IdeBundle.message("run.anything.indexing.mode.not.supported"));
                return;
              }

              if (!myDone.isRejected()) {
                myResultsList.setModel(myListModel);
              }
            }, LIST_REBUILD_DELAY);
          }
          else {
            myResultsList.setModel(myListModel);
          }
        });

        if (myPattern.trim().length() == 0) {
          buildGroups(true);
          return;
        }

        if (isHelpMode(mySearchField.getText())) {
          buildHelpGroups(myListModel);
          updatePopup();
          return;
        }

        check();
        buildGroups(false);
      }
      catch (ProcessCanceledException ignore) {
        myDone.setRejected();
      }
      catch (Exception e) {
        LOG.error(e);
        myDone.setRejected();
      }
      finally {
        if (!isCanceled()) {
          ApplicationManager.getApplication()
            .invokeLater(() -> myResultsList.getEmptyText().setText(IdeBundle.message("run.anything.command.empty.list.title")));
        }
        if (!myDone.isProcessed()) {
          myDone.setDone();
        }
      }
    }

    private void buildGroups(boolean isRecent) {
      buildAllGroups(myPattern, () -> check(), isRecent);
      updatePopup();
    }

    private void buildHelpGroups(@NotNull RunAnythingSearchListModel listModel) {
      listModel.getGroups().forEach(group -> {
        group.collectItems(myDataContext, myListModel, trimHelpPattern(), () -> check());
        check();
      });
    }

    protected void check() {
      myProgressIndicator.checkCanceled();
      if (myDone.isRejected()) throw new ProcessCanceledException();
      assert myCalcThread == this : "There are two CalcThreads running before one of them was cancelled";
    }

    private void buildAllGroups(@NotNull String pattern, @NotNull Runnable checkCancellation, boolean isRecent) {
      if (isRecent) {
        RunAnythingRecentGroup.INSTANCE.collectItems(myDataContext, myListModel, pattern, checkCancellation);
      }
      else {
        buildCompletionGroups(pattern, checkCancellation);
      }
    }

    private void buildCompletionGroups(@NotNull String pattern, @NotNull Runnable checkCancellation) {
      LOG.assertTrue(myListModel instanceof RunAnythingSearchListModel.RunAnythingMainListModel);

      if (DumbService.getInstance(myProject).isDumb()) {
        return;
      }

      StreamEx.of(RunAnythingRecentGroup.INSTANCE)
        .select(RunAnythingGroup.class)
        .append(myListModel.getGroups().stream()
                  .filter(group -> group instanceof RunAnythingCompletionGroup || group instanceof RunAnythingGeneralGroup)
                  .filter(group -> RunAnythingCache.getInstance(myProject).isGroupVisible(group.getTitle())))
        .forEach(group -> {
          ApplicationManager.getApplication().runReadAction(
            () -> group.collectItems(myDataContext, myListModel, pattern, checkCancellation));
          checkCancellation.run();
        });
    }

    private boolean isCanceled() {
      return myProgressIndicator.isCanceled() || myDone.isRejected();
    }

    void updatePopup() {
      ApplicationManager.getApplication().invokeLater(() -> {
        myListModel.update();
        myResultsList.revalidate();
        myResultsList.repaint();

        installScrollingActions();

        updateViewType(myListModel.size() == 0 ? ViewType.SHORT : ViewType.FULL);
      });
    }

    public ActionCallback cancel() {
      myProgressIndicator.cancel();
      return myDone;
    }

    public ActionCallback insert(final int index, @NotNull RunAnythingGroup group) {
      ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(() -> {
        try {
          RunAnythingGroup.SearchResult result = group.getItems(myDataContext, myListModel, trimHelpPattern(), true, this::check);

          check();
          ApplicationManager.getApplication().invokeLater(() -> {
            try {
              int shift = 0;
              int i = index + 1;
              for (Object o : result) {
                myListModel.insertElementAt(o, i);
                shift++;
                i++;
              }

              myListModel.shiftIndexes(index, shift);
              if (!result.isNeedMore()) {
                group.resetMoreIndex();
              }

              clearSelection();
              ScrollingUtil.selectItem(myResultsList, index);
              myDone.setDone();
            }
            catch (Exception e) {
              myDone.setRejected();
            }
          });
        }
        catch (Exception e) {
          myDone.setRejected();
        }
      }));
      return myDone;
    }

    @NotNull
    public String trimHelpPattern() {
      return isHelpMode(myPattern) ? myPattern.substring(HELP_PLACEHOLDER.length()) : myPattern;
    }

    public ActionCallback start() {
      ApplicationManager.getApplication().executeOnPooledThread(this);
      return myDone;
    }
  }

  @Override
  public void installScrollingActions() {
    RunAnythingScrollingUtil.installActions(myResultsList, getField(), () -> {
      myIsItemSelected = true;
      mySearchField.setText(myLastInputText);
      clearSelection();
    }, UISettings.getInstance().getCycleScrolling());

    super.installScrollingActions();
  }

  protected void resetFields() {
    myCurrentWorker.doWhenProcessed(() -> {
      final Object lock = myCalcThread;
      if (lock != null) {
        synchronized (lock) {
          myCurrentWorker = ActionCallback.DONE;
          myCalcThread = null;
          myVirtualFile = null;
          myProject = null;
          myModule = null;
        }
      }
    });
    mySkipFocusGain = false;
  }

  public RunAnythingPopupUI(@NotNull AnActionEvent actionEvent) {
    super(actionEvent.getProject());

    myActionEvent = actionEvent;

    myCurrentWorker = ActionCallback.DONE;
    myVirtualFile = actionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

    myProject = ObjectUtils.notNull(myActionEvent.getData(CommonDataKeys.PROJECT));
    myDataContext = createDataContext(actionEvent);
    myModule = myActionEvent.getData(LangDataKeys.MODULE);

    init();

    initSearchActions();

    initResultsList();

    initSearchField();

    initMySearchField();
  }

  @NotNull
  @Override
  public JBList<Object> createList() {
    myListModel = new RunAnythingSearchListModel.RunAnythingMainListModel();
    addListDataListener(myListModel);

    return new JBList<>(myListModel);
  }

  private void initSearchActions() {
    myResultsList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        onMouseClicked(e);
      }
    });

    DumbAwareAction.create(e -> RunAnythingUtil.jumpNextGroup(true, myResultsList))
      .registerCustomShortcutSet(CustomShortcutSet.fromString("TAB"), mySearchField, this);
    DumbAwareAction.create(e -> RunAnythingUtil.jumpNextGroup(false, myResultsList))
      .registerCustomShortcutSet(CustomShortcutSet.fromString("shift TAB"), mySearchField, this);

    AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
    DumbAwareAction.create(__ -> {
      triggerUsed();
      searchFinishedHandler.run();
    }).registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), this);

    DumbAwareAction.create(e -> executeCommand())
      .registerCustomShortcutSet(
        CustomShortcutSet.fromString("ENTER", "shift ENTER", "alt ENTER", "alt shift ENTER", "meta ENTER"), mySearchField, this);

    DumbAwareAction.create(e -> {
      RunAnythingSearchListModel model = getSearchingModel(myResultsList);
      if (model == null) return;

      Object selectedValue = myResultsList.getSelectedValue();
      int index = myResultsList.getSelectedIndex();
      if (!(selectedValue instanceof RunAnythingItem) || isMoreItem(index)) return;

      RunAnythingCache.getInstance(getProject()).getState().getCommands().remove(((RunAnythingItem)selectedValue).getCommand());

      model.remove(index);
      model.shiftIndexes(index, -1);
      if (model.size() > 0) ScrollingUtil.selectItem(myResultsList, index < model.size() ? index : index - 1);

      ApplicationManager.getApplication().invokeLater(() -> {
        if (myCalcThread != null) {
          myCalcThread.updatePopup();
        }
      });
    }).registerCustomShortcutSet(CustomShortcutSet.fromString("shift BACK_SPACE"), mySearchField, this);

    myProject.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @Override
      public void exitDumbMode() {
        ApplicationManager.getApplication().invokeLater(() -> rebuildList());
      }
    });
  }

  @NotNull
  @Override
  protected ListCellRenderer<Object> createCellRenderer() {
    return new MyListRenderer();
  }

  @NotNull
  @Override
  protected JPanel createSettingsPanel() {
    JPanel res = new JPanel();
    BoxLayout bl = new BoxLayout(res, BoxLayout.X_AXIS);
    res.setLayout(bl);
    res.setOpaque(false);

    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.addAction(new RunAnythingShowFilterAction());

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("search.everywhere.toolbar", actionGroup, true);
    toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    toolbar.updateActionsImmediately();
    JComponent toolbarComponent = toolbar.getComponent();
    toolbarComponent.setOpaque(false);
    toolbarComponent.setBorder(JBUI.Borders.empty(2, 18, 2, 9));
    res.add(toolbarComponent);
    return res;
  }

  @NotNull
  @Override
  protected String getInitialHint() {
    return IdeBundle.message("run.anything.hint.initial.text",
                             KeymapUtil.getKeystrokeText(UP_KEYSTROKE),
                             KeymapUtil.getKeystrokeText(DOWN_KEYSTROKE));
  }

  @NotNull
  @Override
  protected ExtendableTextField createSearchField() {
    ExtendableTextField searchField = super.createSearchField();

    Consumer<? super ExtendableTextComponent.Extension> extensionConsumer = (extension) -> searchField.addExtension(extension);
    searchField.addPropertyChangeListener(new RunAnythingIconHandler(extensionConsumer, searchField));

    return searchField;
  }

  @Override
  public void dispose() {
    resetFields();
  }

  private class RunAnythingShowFilterAction extends ShowFilterAction {

    @NotNull
    @Override
    public String getDimensionServiceKey() {
      return "RunAnythingAction_Filter_Popup";
    }

    @Override
    protected boolean isEnabled() {
      return true;
    }

    @Override
    protected boolean isActive() {
      return RunAnythingCompletionGroup.MAIN_GROUPS.size() != getVisibleGroups().size();
    }

    @Override
    protected ElementsChooser<?> createChooser() {
      ElementsChooser<RunAnythingGroup> res =
        new ElementsChooser<RunAnythingGroup>(new ArrayList<>(RunAnythingCompletionGroup.MAIN_GROUPS), false) {
          @Override
          protected String getItemText(@NotNull RunAnythingGroup value) {
            return value.getTitle();
          }
        };

      res.markElements(getVisibleGroups());
      ElementsChooser.ElementsMarkListener<RunAnythingGroup> listener = (element, isMarked) -> {
        RunAnythingCache.getInstance(myProject).saveGroupVisibilityKey(element.getTitle(), isMarked);
        rebuildList();
      };
      res.addElementsMarkListener(listener);
      return res;
    }

    @NotNull
    private List<RunAnythingGroup> getVisibleGroups() {
      Collection<RunAnythingGroup> groups = RunAnythingCompletionGroup.MAIN_GROUPS;
      return ContainerUtil.filter(groups, group -> RunAnythingCache.getInstance(myProject).isGroupVisible(group.getTitle()));
    }
  }
}
