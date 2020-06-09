// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.FindBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.BigPopupUI;
import com.intellij.ide.actions.bigPopup.ShowFilterAction;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.ide.actions.runAnything.ui.RunAnythingScrollingUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
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
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.BooleanFunction;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.intellij.ide.actions.runAnything.RunAnythingAction.ALT_IS_PRESSED;
import static com.intellij.ide.actions.runAnything.RunAnythingAction.SHIFT_IS_PRESSED;
import static com.intellij.ide.actions.runAnything.RunAnythingIconHandler.MATCHED_PROVIDER_PROPERTY;
import static com.intellij.ide.actions.runAnything.RunAnythingSearchListModel.RunAnythingMainListModel;
import static com.intellij.openapi.wm.IdeFocusManager.getGlobalInstance;
import static java.awt.FlowLayout.RIGHT;

public class RunAnythingPopupUI extends BigPopupUI {
  public static final int SEARCH_FIELD_COLUMNS = 25;
  public static final Icon UNKNOWN_CONFIGURATION_ICON = AllIcons.Actions.Run_anything;
  static final String RUN_ANYTHING = "RunAnything";
  public static final KeyStroke DOWN_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
  public static final KeyStroke UP_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
  private static final Border RENDERER_BORDER = JBUI.Borders.empty(1, 0);
  private static final String HELP_PLACEHOLDER = "?";
  private boolean myIsUsedTrigger;
  private volatile ActionCallback myCurrentWorker;
  private boolean mySkipFocusGain = false;
  @Nullable private final VirtualFile myVirtualFile;
  private JLabel myTextFieldTitle;
  private boolean myIsItemSelected;
  private String myLastInputText = null;
  private final Project myProject;
  private final Module myModule;

  private RunAnythingContext mySelectedExecutingContext;
  private final List<RunAnythingContext> myAvailableExecutingContexts = new ArrayList<>();
  private RunAnythingChooseContextAction myChooseContextAction;
  private final Alarm myListRenderingAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final ExecutorService myExecutorService =
    SequentialTaskExecutor.createSequentialApplicationPoolExecutor("Run Anything list building");

  @Nullable
  public String getUserInputText() {
    return myResultsList.getSelectedIndex() >= 0 ? myLastInputText : mySearchField.getText();
  }

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
    updateContextCombobox();
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

            //invoke later here allows to get correct pattern from mySearchField
            ApplicationManager.getApplication().invokeLater(() -> {
              rebuildList();
            });
          }

          if (!isHelpMode(pattern)) {
            updateContextCombobox();
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
        rebuildList();
      }

      @Override
      public void focusLost(FocusEvent e) {
        searchFinishedHandler.run();
      }
    });
  }

  private static void adjustMainListEmptyText(@NotNull JBTextField editor) {
    adjustEmptyText(editor, field -> field.getText().isEmpty(), IdeBundle.message("run.anything.main.list.empty.primary.text"),
                    IdeBundle.message("run.anything.main.list.empty.secondary.text"));
  }

  static boolean isHelpMode(@NotNull String pattern) {
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

    final RunAnythingSearchListModel model = getSearchingModel(myResultsList);
    if (index != -1 && model != null && isMoreItem(index)) {
      RunAnythingGroup group = model.findGroupByMoreIndex(index);

      if (group != null) {
        myCurrentWorker.doWhenProcessed(() -> {
          RunAnythingUsageCollector.Companion.triggerMoreStatistics(myProject, group, model.getClass());
          RunAnythingSearchListModel listModel = (RunAnythingSearchListModel)myResultsList.getModel();
          myCurrentWorker = insert(group, listModel, getDataContext(), getSearchPattern(), index, -1);
          myCurrentWorker.doWhenProcessed(() -> {
            clearSelection();
            ScrollingUtil.selectItem(myResultsList, index);
          });
        });

        return;
      }
    }

    if (model != null) {
      RunAnythingUsageCollector.Companion.triggerExecCategoryStatistics(myProject, model.getGroups(), model.getClass(), index,
                                                                        SHIFT_IS_PRESSED.get(), ALT_IS_PRESSED.get());
    }
    RunAnythingUtil.executeMatched(getDataContext(), pattern);

    mySearchField.setText("");
    searchFinishedHandler.run();
    triggerUsed();
  }

  @NotNull
  public static ActionCallback insert(@NotNull RunAnythingGroup group,
                                      @NotNull RunAnythingSearchListModel listModel,
                                      @NotNull DataContext dataContext,
                                      @NotNull String pattern,
                                      int index,
                                      int itemsNumberToInsert) {
    ActionCallback callback = new ActionCallback();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      List<RunAnythingItem> items = StreamEx.of(listModel.getItems()).select(RunAnythingItem.class).collect(Collectors.toList());
      RunAnythingGroup.SearchResult result;
      try {
        result = ProgressManager.getInstance().runProcess(
          () -> group.getItems(dataContext,
                               items,
                               trimHelpPattern(pattern),
                               itemsNumberToInsert == -1 ? group.getMaxItemsToInsert() : itemsNumberToInsert),
          new EmptyProgressIndicator());
      }
      catch (ProcessCanceledException e) {
        callback.setRejected();
        return;
      }

      ApplicationManager.getApplication().invokeLater(() -> {
        int shift = 0;
        int i = index + 1;
        for (Object o : result) {
          listModel.add(i, o);
          shift++;
          i++;
        }

        listModel.shiftIndexes(index, shift);
        if (!result.isNeedMore()) {
          group.resetMoreIndex();
        }

        callback.setDone();
      });
    });
    return callback;
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
  private VirtualFile getWorkDirectory() {
    if (ALT_IS_PRESSED.get()) {
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

    return getBaseDirectory(getModule());
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
    ApplicationManager.getApplication().assertIsDispatchThread();

    myListRenderingAlarm.cancelAllRequests();
    myResultsList.getEmptyText().setText(FindBundle.message("empty.text.searching"));

    if (DumbService.getInstance(myProject).isDumb()) {
      myResultsList.setEmptyText(IdeBundle.message("run.anything.indexing.mode.not.supported"));
      return;
    }

    ReadAction.nonBlocking(new RunAnythingCalcThread(myProject, getDataContext(), getSearchPattern())::compute)
      .coalesceBy(this)
      .finishOnUiThread(ModalityState.defaultModalityState(), model ->
        myListRenderingAlarm.addRequest(() -> {
          addListDataListener(model);
          myResultsList.setModel(model);
          model.update();
        }, 150))
      .submit(myExecutorService);
  }

  @Override
  protected void addListDataListener(@NotNull AbstractListModel<Object> model) {
    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        updateViewType(ViewType.FULL);
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        if (myResultsList.isEmpty()) {
          updateViewType(ViewType.SHORT);
        }
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        updateViewType(myResultsList.isEmpty() ? ViewType.SHORT : ViewType.FULL);
      }
    });
  }

  protected void resetFields() {
    mySkipFocusGain = false;
  }

  public void initResultsList() {
    myResultsList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        updateAdText(getDataContext());

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

  private void updateContextCombobox() {
    DataContext dataContext = getDataContext();
    Object value = myResultsList.getSelectedValue();
    String text = value instanceof RunAnythingItem ? ((RunAnythingItem)value).getCommand() : getSearchPattern();
    RunAnythingProvider provider = RunAnythingProvider.findMatchedProvider(dataContext, text);
    if (provider != null) {
      myChooseContextAction.setAvailableContexts(provider.getExecutionContexts(dataContext));
    }

    AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext);
    ActionUtil.performDumbAwareUpdate(false, myChooseContextAction, event, false);
  }

  @Override
  @NotNull
  public JPanel createTopLeftPanel() {
    myTextFieldTitle = new JLabel(IdeBundle.message("run.anything.run.anything.title"));
    JPanel topPanel = new NonOpaquePanel(new BorderLayout());
    Color foregroundColor = StartupUiUtil.isUnderDarcula()
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
  private DataContext getDataContext() {
    HashMap<String, Object> dataMap = new HashMap<>();
    dataMap.put(CommonDataKeys.PROJECT.getName(), getProject());
    dataMap.put(LangDataKeys.MODULE.getName(), getModule());
    dataMap.put(CommonDataKeys.VIRTUAL_FILE.getName(), getWorkDirectory());
    dataMap.put(RunAnythingAction.EXECUTOR_KEY.getName(), getExecutor());
    dataMap.put(RunAnythingProvider.EXECUTING_CONTEXT.getName(), myChooseContextAction.getSelectedContext());
    return SimpleDataContext.getSimpleContext(dataMap, null);
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
    statusText.setShowAboveCenter(false);
    statusText.setText(leftText, SimpleTextAttributes.GRAY_ATTRIBUTES);
    statusText.appendText(false, 0, rightText, SimpleTextAttributes.GRAY_ATTRIBUTES, null);
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

    DataContext dataContext = getDataContext();
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
          cmp = ((RunAnythingItem)value).createComponent(myLastInputText, isSelected, hasFocus);
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
          myMainPanel.add(RunAnythingUtil.createTitle(" " + title, UIUtil.getListBackground(false, false)), BorderLayout.NORTH);
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

    @Override
    protected void customizeCellRenderer(@NotNull JList list, final Object value, int index, final boolean selected, boolean hasFocus) {
    }
  }

  @NotNull
  public static String trimHelpPattern(@NotNull String pattern) {
    return isHelpMode(pattern) ? pattern.substring(HELP_PLACEHOLDER.length()) : pattern;
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

  public RunAnythingPopupUI(@NotNull AnActionEvent actionEvent) {
    super(actionEvent.getProject());

    myCurrentWorker = ActionCallback.DONE;
    myVirtualFile = actionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

    myProject = Objects.requireNonNull(actionEvent.getData(CommonDataKeys.PROJECT));
    myModule = actionEvent.getData(LangDataKeys.MODULE);

    init();

    initSearchActions();

    initResultsList();

    initSearchField();

    initMySearchField();
  }

  @NotNull
  @Override
  public JBList<Object> createList() {
    RunAnythingSearchListModel listModel = new RunAnythingMainListModel();
    addListDataListener(listModel);

    return new JBList<>(listModel);
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
      if (!model.isEmpty()) ScrollingUtil.selectItem(myResultsList, index < model.getSize() ? index : index - 1);
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
    JPanel res = new JPanel(new FlowLayout(RIGHT, 0, 0));
    res.setOpaque(false);

    DefaultActionGroup actionGroup = new DefaultActionGroup();
    myChooseContextAction = new RunAnythingChooseContextAction(res) {
      @Override
      public void setAvailableContexts(@NotNull List<? extends RunAnythingContext> executionContexts) {
        myAvailableExecutingContexts.clear();
        myAvailableExecutingContexts.addAll(executionContexts);
      }

      @NotNull
      @Override
      public List<RunAnythingContext> getAvailableContexts() {
        return myAvailableExecutingContexts;
      }

      @Override
      public void setSelectedContext(@Nullable RunAnythingContext context) {
        mySelectedExecutingContext = context;
      }

      @Nullable
      @Override
      public RunAnythingContext getSelectedContext() {
        return mySelectedExecutingContext;
      }
    };
    actionGroup.addAction(myChooseContextAction);
    actionGroup.addAction(new RunAnythingShowFilterAction());

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("run.anything.toolbar", actionGroup, true);
    toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    toolbar.updateActionsImmediately();
    JComponent toolbarComponent = toolbar.getComponent();
    toolbarComponent.setOpaque(false);
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

  @Override
  protected @NotNull String getAccessibleName() {
    return IdeBundle.message("run.anything.accessible.name");
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
    @NotNull private final Collection<RunAnythingGroup> myTemplateGroups;

    private RunAnythingShowFilterAction() {
      myTemplateGroups = RunAnythingCompletionGroup.createCompletionGroups();
    }

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
      return myTemplateGroups.size() != getVisibleGroups().size();
    }

    @Override
    protected ElementsChooser<?> createChooser() {
      ElementsChooser<RunAnythingGroup> res =
        new ElementsChooser<RunAnythingGroup>(new ArrayList<>(myTemplateGroups), false) {
          @Override
          protected String getItemText(@NotNull RunAnythingGroup value) {
            return value.getTitle();
          }
        };

      res.markElements(getVisibleGroups());
      ElementsChooser.ElementsMarkListener<RunAnythingGroup> listener = (element, isMarked) -> {
        RunAnythingCache.getInstance(myProject)
          .saveGroupVisibilityKey(element instanceof RunAnythingCompletionGroup
                                  ? ((RunAnythingCompletionGroup)element).getProvider().getClass().getCanonicalName()
                                  : element.getTitle(), isMarked);
        rebuildList();
      };
      res.addElementsMarkListener(listener);
      return res;
    }

    @NotNull
    private List<RunAnythingGroup> getVisibleGroups() {
      return ContainerUtil.filter(myTemplateGroups, group -> RunAnythingCache.getInstance(myProject).isGroupVisible(group));
    }
  }
}
