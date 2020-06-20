// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.CommonBundle;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.find.*;
import com.intellij.find.actions.ShowUsagesAction;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.TooltipLinkProvider;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ReadTask;
import com.intellij.openapi.project.*;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import com.intellij.pom.Navigatable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopeUtil;
import com.intellij.reference.SoftReference;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.mac.TouchbarDataKeys;
import com.intellij.ui.popup.util.PopupState;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.JBTable;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.*;
import com.intellij.usages.impl.UsagePreviewPanel;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.*;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.intellij.ui.SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;
import static com.intellij.util.FontUtil.spaceAndThinSpace;

public class FindPopupPanel extends JBPanel<FindPopupPanel> implements FindUI {
  private static final Logger LOG = Logger.getInstance(FindPopupPanel.class);

  private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
  private static final KeyStroke ENTER_WITH_MODIFIERS = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, SystemInfo.isMac
                                                                                                  ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);
  private static final KeyStroke REPLACE_ALL = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_MASK);
  private static final KeyStroke RESET_FILTERS = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK);

  private static final String FIND_TYPE = "FindInPath";
  private static final String SERVICE_KEY = "find.popup";
  private static final String SPLITTER_SERVICE_KEY = "find.popup.splitter";
  @NotNull private final FindUIHelper myHelper;
  @NotNull private final Project myProject;
  @NotNull private final Disposable myDisposable;
  private final Alarm myPreviewUpdater;
  @NotNull private final FindPopupScopeUI myScopeUI;
  private JComponent myCodePreviewComponent;
  private SearchTextArea mySearchTextArea;
  private SearchTextArea myReplaceTextArea;
  private ActionListener myOkActionListener;
  private final AtomicBoolean myCanClose = new AtomicBoolean(true);
  private final AtomicBoolean myIsPinned = new AtomicBoolean(false);
  private JBLabel myOKHintLabel;
  private JBLabel myNavigationHintLabel;
  private Alarm mySearchRescheduleOnCancellationsAlarm;
  private volatile ProgressIndicatorBase myResultsPreviewSearchProgress;

  private JLabel myTitleLabel;
  private JLabel myInfoLabel;
  private final AtomicBoolean myCaseSensitiveState = new AtomicBoolean();
  private final AtomicBoolean myPreserveCaseState = new AtomicBoolean();
  private final AtomicBoolean myWholeWordsState = new AtomicBoolean();
  private final AtomicBoolean myRegexState = new AtomicBoolean();
  private final List<AnAction> myExtraActions = new ArrayList<>();
  private StateRestoringCheckBox myCbFileFilter;
  private ActionToolbarImpl myScopeSelectionToolbar;
  private ComboBox<String> myFileMaskField;
  private ActionButton myFilterContextButton;
  private JButton myOKButton;
  private JButton myReplaceAllButton;
  private JButton myReplaceSelectedButton;
  private JTextArea mySearchComponent;
  private JTextArea myReplaceComponent;
  private String mySelectedContextName = FindBundle.message("find.context.anywhere.scope.label");
  private FindPopupScopeUI.ScopeType mySelectedScope;
  private JPanel myScopeDetailsPanel;

  private JBTable myResultsPreviewTable;
  private DefaultTableModel myResultsPreviewTableModel;
  private SimpleColoredComponent myUsagePreviewTitle;
  private UsagePreviewPanel myUsagePreviewPanel;
  private DialogWrapper myDialog;
  private LoadingDecorator myLoadingDecorator;
  private int myLoadingHash;
  private final AtomicBoolean myNeedReset = new AtomicBoolean(true);
  private JPanel myTitlePanel;
  private String myUsagesCount;
  private String myFilesCount;
  private UsageViewPresentation myUsageViewPresentation;
  private final ComponentValidator myComponentValidator;
  private AnAction myCaseSensitiveAction;
  private AnAction myWholeWordsAction;
  private AnAction myRegexAction;
  private AnAction myResetFiltersAction;
  private boolean mySuggestRegexHintForEmptyResults = true;

  FindPopupPanel(@NotNull FindUIHelper helper) {
    myHelper = helper;
    myProject = myHelper.getProject();
    myDisposable = Disposer.newDisposable();
    myPreviewUpdater = new Alarm(myDisposable);
    myScopeUI = FindPopupScopeUIProvider.getInstance().create(this);
    myComponentValidator = new ComponentValidator(myDisposable) {
      @Override
      public void updateInfo(@Nullable ValidationInfo info) {
        if (info != null && info.component == mySearchComponent) {
          super.updateInfo(null);
        }
        else {
        super.updateInfo(info);
        }
      }
    };

    Disposer.register(myDisposable, () -> {
      finishPreviousPreviewSearch();
      if (mySearchRescheduleOnCancellationsAlarm != null) Disposer.dispose(mySearchRescheduleOnCancellationsAlarm);
      if (myUsagePreviewPanel != null) Disposer.dispose(myUsagePreviewPanel);
    });

    initComponents();
    initByModel();

    FindUtil.triggerUsedOptionsStats(FIND_TYPE, myHelper.getModel());
  }

  @Override
  public void showUI() {
    if (myDialog != null && myDialog.isVisible()) {
      return;
    }
    if (myDialog != null && !Disposer.isDisposed(myDialog.getDisposable())) {
      myDialog.doCancelAction();
    }
    if (myDialog == null || Disposer.isDisposed(myDialog.getDisposable())) {
      myDialog = new DialogWrapper(myHelper.getProject(), null, true, DialogWrapper.IdeModalityType.MODELESS, false) {
        {
          init();
          getRootPane().setDefaultButton(null);
        }

        @Override
        protected void doOKAction() {
          processCtrlEnter();
        }

        @Override
        protected void dispose() {
          saveSettings();
          super.dispose();
        }

        @Nullable
        @Override
        protected Border createContentPaneBorder() {
          return null;
        }

        @Override
        protected JComponent createCenterPanel() {
          return FindPopupPanel.this;
        }

        @Override
        protected String getDimensionServiceKey() {
          return SERVICE_KEY;
        }
      };
      myDialog.setUndecorated(!Registry.is("ide.find.as.popup.decorated"));
      ApplicationManager.getApplication().getMessageBus().connect(myDialog.getDisposable()).subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
        @Override
        public void projectClosed(@NotNull Project project) {
          closeImmediately();
        }
      });
      Disposer.register(myDialog.getDisposable(), myDisposable);

      final Window window = WindowManager.getInstance().suggestParentWindow(myProject);
      Component parent = UIUtil.findUltimateParent(window);
      RelativePoint showPoint = null;
      Point screenPoint = DimensionService.getInstance().getLocation(SERVICE_KEY);
      if (screenPoint != null) {
        if (parent != null) {
          SwingUtilities.convertPointFromScreen(screenPoint, parent);
          showPoint = new RelativePoint(parent, screenPoint);
        }
        else {
          showPoint = new RelativePoint(screenPoint);
        }
      }
      if (parent != null && showPoint == null) {
        int height = UISettings.getInstance().getShowNavigationBar() ? 135 : 115;
        if (parent instanceof IdeFrameImpl && ((IdeFrameImpl)parent).isInFullScreen()) {
          height -= 20;
        }
        showPoint = new RelativePoint(parent, new Point((parent.getSize().width - getPreferredSize().width) / 2, height));
      }
      ApplicationManager.getApplication().invokeLater(() -> {
        if (mySearchComponent.getCaret() != null) {
          mySearchComponent.selectAll();
        }
      });
      WindowMoveListener windowListener = new WindowMoveListener(this);
      myTitlePanel.addMouseListener(windowListener);
      myTitlePanel.addMouseMotionListener(windowListener);
      addMouseListener(windowListener);
      addMouseMotionListener(windowListener);
      Dimension panelSize = getPreferredSize();
      Dimension prev = DimensionService.getInstance().getSize(SERVICE_KEY);
      panelSize.width += JBUIScale.scale(24);//hidden 'loading' icon
      panelSize.height *= 2;
      if (prev != null && prev.height < panelSize.height) prev.height = panelSize.height;
      Window w = myDialog.getPeer().getWindow();
      final AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
      JRootPane root = ((RootPaneContainer)w).getRootPane();

      IdeGlassPaneImpl glass = (IdeGlassPaneImpl)myDialog.getRootPane().getGlassPane();
      int i = Registry.intValue("ide.popup.resizable.border.sensitivity", 4);
      WindowResizeListener resizeListener = new WindowResizeListener(
        root,
        JBUI.insets(i),
        null) {
        private Cursor myCursor;

        @Override
        protected void setCursor(Component content, Cursor cursor) {
          if (myCursor != cursor || myCursor != Cursor.getDefaultCursor()) {
            glass.setCursor(cursor, this);
            myCursor = cursor;

            if (content instanceof JComponent) {
              IdeGlassPaneImpl.savePreProcessedCursor((JComponent)content, content.getCursor());
            }
            super.setCursor(content, cursor);
          }
        }
      };
      glass.addMousePreprocessor(resizeListener, myDisposable);
      glass.addMouseMotionPreprocessor(resizeListener, myDisposable);

      DumbAwareAction.create(e -> closeImmediately())
        .registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), root, myDisposable);
      root.setWindowDecorationStyle(JRootPane.NONE);
      root.setBorder(PopupBorder.Factory.create(true, true));
      UIUtil.markAsPossibleOwner((Dialog)w);
      w.setBackground(UIUtil.getPanelBackground());
      w.setMinimumSize(panelSize);
      if (prev == null) {
        panelSize.height *= 1.5;
        panelSize.width *= 1.15;
      }
      w.setSize(prev != null ? prev : panelSize);

      IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
      if (showPoint != null) {
        myDialog.setLocation(showPoint.getScreenPoint());
      }
      else {
        w.setLocationRelativeTo(parent);
      }
      mySuggestRegexHintForEmptyResults = true;
      myDialog.show();

      w.addWindowListener(new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
          w.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
              Window oppositeWindow = e.getOppositeWindow();
              if (oppositeWindow == w || oppositeWindow != null && oppositeWindow.getOwner() == w) {
                return;
              }
              if (canBeClosed() || !myIsPinned.get() && oppositeWindow != null) {
                //closeImmediately();
                myDialog.doCancelAction();
              }
            }
          });
        }
      });

      JRootPane rootPane = getRootPane();
      if (rootPane != null) {
        if (myHelper.isReplaceState()) {
          rootPane.setDefaultButton(myReplaceSelectedButton);
        }
        rootPane.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ENTER_WITH_MODIFIERS, "openInFindWindow");
        rootPane.getActionMap().put("openInFindWindow", new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            processCtrlEnter();
          }
        });
      }
      ApplicationManager.getApplication().invokeLater(this::scheduleResultsUpdate, ModalityState.any());
    }
  }

  protected boolean canBeClosed() {
    if (myProject.isDisposed()) return true;
    if (!myCanClose.get()) return false;
    if (myIsPinned.get()) return false;
    if (!ApplicationManager.getApplication().isActive()) return false;
    if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null) return false;
    if (myFileMaskField.isPopupVisible()) {
      myFileMaskField.setPopupVisible(false);
      return false;
    }
    List<JBPopup> popups = ContainerUtil.filter(JBPopupFactory.getInstance().getChildPopups(this), popup -> !popup.isDisposed());
    if (!popups.isEmpty()) {
      for (JBPopup popup : popups) {
        popup.cancel();
      }
      return false;
    }
    return !myScopeUI.hideAllPopups();
  }

  @Override
  public void saveSettings() {
    DimensionService.getInstance().setSize(SERVICE_KEY, myDialog.getSize(), myHelper.getProject() );
    DimensionService.getInstance().setLocation(SERVICE_KEY, myDialog.getWindow().getLocationOnScreen(), myHelper.getProject() );
    FindSettings findSettings = FindSettings.getInstance();
    myScopeUI.applyTo(findSettings, mySelectedScope);
    myHelper.updateFindSettings();
    applyTo(FindManager.getInstance(myProject).getFindInProjectModel());
  }

  @NotNull
  @Override
  public Disposable getDisposable() {
    return myDisposable;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public FindUIHelper getHelper() {
    return myHelper;
  }

  @NotNull
  public AtomicBoolean getCanClose() {
    return myCanClose;
  }

  private void initComponents() {
    myTitleLabel = new JBLabel(FindBundle.message("find.in.path.dialog.title"), UIUtil.ComponentStyle.REGULAR);
    RelativeFont.BOLD.install(myTitleLabel);
    myInfoLabel = new JBLabel("", UIUtil.ComponentStyle.SMALL);
    myLoadingDecorator = new LoadingDecorator(new JLabel(EmptyIcon.ICON_16), getDisposable(), 250, true, new AsyncProcessIcon("FindInPathLoading"));
    myLoadingDecorator.setLoadingText("");
    ItemListener liveResultsPreviewUpdateListener = __ -> scheduleResultsUpdate();
    myCbFileFilter = createCheckBox("find.popup.filemask", "FileFilter");
    myCbFileFilter.addItemListener(__ -> {
      if (myCbFileFilter.isSelected()) {
        myFileMaskField.setEnabled(true);
        if (myCbFileFilter.getClientProperty("dontRequestFocus") == null) {
          IdeFocusManager.getInstance(myProject).requestFocus(myFileMaskField, true);
          myFileMaskField.getEditor().selectAll();
        }
      }
      else {
        myFileMaskField.setEnabled(false);
        if (myCbFileFilter.getClientProperty("dontRequestFocus") == null) {
          IdeFocusManager.getInstance(myProject).requestFocus(mySearchComponent, true);
        }
      }
    });
    myCbFileFilter.addItemListener(liveResultsPreviewUpdateListener);
    myFileMaskField = new ComboBox<String>() {
      @Override
      public Dimension getPreferredSize() {
        int width = 0;
        int buttonWidth = 0;
        Component[] components = getComponents();
        for (Component component : components) {
          Dimension size = component.getPreferredSize();
          int w = size != null ? size.width : 0;
          if (component instanceof JButton) {
            buttonWidth = w;
          }
          width += w;
        }
        ComboBoxEditor editor = getEditor();
        if (editor != null) {
          Component editorComponent = editor.getEditorComponent();
          if (editorComponent != null) {
            FontMetrics fontMetrics = editorComponent.getFontMetrics(editorComponent.getFont());
            width = Math.max(width, fontMetrics.stringWidth(String.valueOf(getSelectedItem())) + buttonWidth);
            //Let's reserve some extra space for just one 'the next' letter
            width += fontMetrics.stringWidth("m");
          }
        }
        Dimension size = super.getPreferredSize();
        Insets insets = getInsets();
        width += insets.left + insets.right;
        size.width = MathUtil.clamp(width, JBUIScale.scale(80), JBUIScale.scale(500));
        return size;
      }
    };
    myFileMaskField.setEditable(true);
    myFileMaskField.setMaximumRowCount(8);
    myFileMaskField.addActionListener(__ -> scheduleResultsUpdate());
    Component editorComponent = myFileMaskField.getEditor().getEditorComponent();
    if (editorComponent instanceof EditorTextField) {
      final EditorTextField etf = (EditorTextField) editorComponent;
      etf.addDocumentListener(new DocumentListener() {
        @Override
        public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
          onFileMaskChanged();
        }
      });
    }
    else {
      if (editorComponent instanceof JTextComponent) {
        ((JTextComponent)editorComponent).getDocument().addDocumentListener(new DocumentAdapter() {
          @Override
          protected void textChanged(@NotNull DocumentEvent e) {
            onFileMaskChanged();
          }
        });
      }
      else {
        assert false;
      }
    }

    AnAction myShowFilterPopupAction = new MyShowFilterPopupAction();
    myFilterContextButton =
      new ActionButton(myShowFilterPopupAction, myShowFilterPopupAction.getTemplatePresentation(), ActionPlaces.UNKNOWN,
                       ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {
        @Override
        public Icon getIcon() {
          Icon icon = myShowFilterPopupAction.getTemplatePresentation().getIcon();
          return mySelectedContextName.equals(FindInProjectUtil.getPresentableName(FindModel.SearchContext.ANY))
                 ? icon
                 : ExecutionUtil.getLiveIndicator(icon);
        }
      };
    myShowFilterPopupAction.registerCustomShortcutSet(myShowFilterPopupAction.getShortcutSet(), this);
    ToggleAction pinAction = new MyPinAction();
    ActionButton pinButton =
      new ActionButton(pinAction, pinAction.getTemplatePresentation(), ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);

    DefaultActionGroup tabOptionsGroup = new DefaultActionGroup() {{
      Presentation presentation = getTemplatePresentation();
      presentation.setText(FindBundle.messagePointer("find.popup.show.tab.options"));
      presentation.setIcon(AllIcons.General.GearPlain);
      setPopup(true);
      setShortcutSet(CustomShortcutSet.fromString("alt ctrl DOWN"));
      add(new MySkipTabWithOneUsageAction());
      add(new MyOpenResultsInNewTabAction());
    }};
    ActionButton tabOptionsButton = new ActionButton(
      tabOptionsGroup, tabOptionsGroup.getTemplatePresentation(), ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    );
    DumbAwareAction.create(event -> tabOptionsButton.click()).registerCustomShortcutSet(tabOptionsGroup.getShortcutSet(), this);
    myResetFiltersAction = DumbAwareAction.create(event -> resetAllFilters());
    myResetFiltersAction.registerCustomShortcutSet(new CustomShortcutSet(RESET_FILTERS), this);

    myOKButton = new JButton(FindBundle.message("find.popup.find.button"));
    myReplaceAllButton = new JButton(FindBundle.message("find.popup.replace.all.button"));
    myReplaceSelectedButton = new JButton(FindBundle.message("find.popup.replace.selected.button", 0));

    myOkActionListener = __ -> doOK(true);
    myReplaceAllButton.addActionListener(__ -> doOK(false));
    myReplaceSelectedButton.addActionListener(e -> {
      int rowToSelect = myResultsPreviewTable.getSelectionModel().getMinSelectionIndex();
      Map<Integer, Usage> usages = getSelectedUsages();
      if (usages == null) {
        return;
      }
      CommandProcessor.getInstance().executeCommand(myProject, () -> {
        for (Map.Entry<Integer, Usage> entry : usages.entrySet()) {
          try {
            ReplaceInProjectManager.getInstance(myProject).replaceUsage(entry.getValue(), myHelper.getModel(), Collections.emptySet(), false);
            ((DefaultTableModel)myResultsPreviewTable.getModel()).removeRow(entry.getKey());
          }
          catch (FindManager.MalformedReplacementStringException ex) {
            if (!ApplicationManager.getApplication().isUnitTestMode()) {
              Messages.showErrorDialog(this, ex.getMessage(), FindBundle.message("find.replace.invalid.replacement.string.title"));
            }
            break;
          }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
          if (myResultsPreviewTable.getRowCount() > rowToSelect) {
            myResultsPreviewTable.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
          }
          ScrollingUtil.ensureSelectionExists(myResultsPreviewTable);
        });
      }, FindBundle.message("find.replace.command"), null);
    });
    myOKButton.addActionListener(myOkActionListener);
    TouchbarDataKeys.putDialogButtonDescriptor(myOKButton, 0, true);
    boolean enterAsOK = Registry.is("ide.find.enter.as.ok", false);

    new MyEnterAction(enterAsOK).registerCustomShortcutSet(new CustomShortcutSet(ENTER), this);
    DumbAwareAction.create(__ -> processCtrlEnter()).registerCustomShortcutSet(new CustomShortcutSet(ENTER_WITH_MODIFIERS), this);
    DumbAwareAction.create(__ -> myReplaceAllButton.doClick()).registerCustomShortcutSet(new CustomShortcutSet(REPLACE_ALL), this);
    myReplaceAllButton.setToolTipText(KeymapUtil.getKeystrokeText(REPLACE_ALL));

    List<Shortcut> navigationKeyStrokes = new ArrayList<>();
    KeyStroke viewSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getViewSource());
    if (viewSourceKeyStroke != null && !Comparing.equal(viewSourceKeyStroke, ENTER_WITH_MODIFIERS) && !Comparing.equal(viewSourceKeyStroke, ENTER)) {
      navigationKeyStrokes.add(new KeyboardShortcut(viewSourceKeyStroke, null));
    }
    KeyStroke editSourceKeyStroke = KeymapUtil.getKeyStroke(CommonShortcuts.getEditSource());
    if (editSourceKeyStroke != null && !Comparing.equal(editSourceKeyStroke, ENTER_WITH_MODIFIERS) && !Comparing.equal(editSourceKeyStroke, ENTER)) {
      navigationKeyStrokes.add(new KeyboardShortcut(editSourceKeyStroke, null));
    }
    if (!navigationKeyStrokes.isEmpty()) {
      DumbAwareAction.create(e -> navigateToSelectedUsage(e))
                     .registerCustomShortcutSet(new CustomShortcutSet(navigationKeyStrokes.toArray(Shortcut.EMPTY_ARRAY)), this);
    }

    mySearchComponent = new JBTextArea();
    mySearchComponent.setColumns(25);
    mySearchComponent.setRows(1);
    mySearchComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.search.accessible.name"));
    myReplaceComponent = new JBTextArea();
    myReplaceComponent.setColumns(25);
    myReplaceComponent.setRows(1);
    myReplaceComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.replace.accessible.name"));
    mySearchTextArea = new SearchTextArea(mySearchComponent, true);
    myReplaceTextArea = new SearchTextArea(myReplaceComponent, false);
     mySearchTextArea.setMultilineEnabled(Registry.is("ide.find.as.popup.allow.multiline"));
    myReplaceTextArea.setMultilineEnabled(Registry.is("ide.find.as.popup.allow.multiline"));
    myCaseSensitiveAction =
      new MySwitchStateToggleAction("find.popup.case.sensitive", "CaseSensitive",
                                    AllIcons.Actions.MatchCase, AllIcons.Actions.MatchCaseHovered, AllIcons.Actions.MatchCaseSelected,
                                    myCaseSensitiveState, () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get());
    myWholeWordsAction =
      new MySwitchStateToggleAction("find.whole.words", "WholeWords",
                                    AllIcons.Actions.Words, AllIcons.Actions.WordsHovered, AllIcons.Actions.WordsSelected,
                                    myWholeWordsState, () -> !myRegexState.get());
    myRegexAction =
      new MySwitchStateToggleAction("find.regex", "Regex",
                                    AllIcons.Actions.Regex, AllIcons.Actions.RegexHovered, AllIcons.Actions.RegexSelected,
                                    myRegexState, () -> !myHelper.getModel().isReplaceState() || !myPreserveCaseState.get(),
                                    Pair.create(FindBundle.message("find.regex.help.link"),
                                                RegExHelpPopup.createRegExLinkRunnable(mySearchTextArea)));
    List<Component> searchExtraButtons =
      mySearchTextArea.setExtraActions(myCaseSensitiveAction, myWholeWordsAction, myRegexAction);
    AnAction preserveCaseAction =
      new MySwitchStateToggleAction("find.options.replace.preserve.case", "PreserveCase",
                     AllIcons.Actions.PreserveCase, AllIcons.Actions.PreserveCaseHover, AllIcons.Actions.PreserveCaseSelected,
                     myPreserveCaseState, () -> !myRegexState.get() && !myCaseSensitiveState.get());
    List<Component> replaceExtraButtons = myReplaceTextArea.setExtraActions(
      preserveCaseAction);
    myExtraActions.addAll(Arrays.asList(myCaseSensitiveAction, myWholeWordsAction, myRegexAction, preserveCaseAction));
    Pair<FindPopupScopeUI.ScopeType, JComponent>[] scopeComponents = myScopeUI.getComponents();

    myScopeDetailsPanel = new JPanel(new CardLayout());
    myScopeDetailsPanel.setBorder(JBUI.Borders.emptyBottom(UIUtil.isUnderDefaultMacTheme() ? 0 : 3));

    List<AnAction> scopeActions = new ArrayList<>(scopeComponents.length);
    for (Pair<FindPopupScopeUI.ScopeType, JComponent> scopeComponent : scopeComponents) {
      FindPopupScopeUI.ScopeType scopeType = scopeComponent.first;
      scopeActions.add(new MySelectScopeToggleAction(scopeType));
      myScopeDetailsPanel.add(scopeType.name, scopeComponent.second);
    }
    myScopeSelectionToolbar = createToolbar(scopeActions.toArray(AnAction.EMPTY_ARRAY));
    myScopeSelectionToolbar.setMinimumButtonSize(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
    mySelectedScope = scopeComponents[0].first;

    myResultsPreviewTableModel = createTableModel();
    myResultsPreviewTable = new JBTable(myResultsPreviewTableModel) {
      @Override
      public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(getWidth(), 1 + getRowHeight() * 4);
      }
    };
    myResultsPreviewTable.setFocusable(false);
    myResultsPreviewTable.getEmptyText().setShowAboveCenter(false);
    myResultsPreviewTable.setShowColumns(false);
    myResultsPreviewTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    myResultsPreviewTable.setShowGrid(false);
    myResultsPreviewTable.setIntercellSpacing(JBUI.emptySize());
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(@NotNull MouseEvent event) {
        if (event.getSource() != myResultsPreviewTable) return false;
        navigateToSelectedUsage(null);
        return true;
      }
    }.installOn(myResultsPreviewTable);
    myResultsPreviewTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        myResultsPreviewTable.transferFocus();
      }
    });
    applyFont(JBFont.label(), myResultsPreviewTable);
    JComponent[] tableAware = {mySearchComponent, myReplaceComponent, myReplaceSelectedButton};
    for (JComponent component : tableAware) {
      ScrollingUtil.installActions(myResultsPreviewTable, false, component);
    }

    ActionListener helpAction = __ -> HelpManager.getInstance().invokeHelp("reference.dialogs.findinpath");
    registerKeyboardAction(helpAction,KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),JComponent.WHEN_IN_FOCUSED_WINDOW);
    registerKeyboardAction(helpAction,KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0),JComponent.WHEN_IN_FOCUSED_WINDOW);
    KeymapManager keymapManager = KeymapManager.getInstance();
    Keymap activeKeymap = keymapManager != null ? keymapManager.getActiveKeymap() : null;
    if (activeKeymap != null) {
      ShortcutSet findNextShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindNext"));
      ShortcutSet findPreviousShortcutSet = new CustomShortcutSet(activeKeymap.getShortcuts("FindPrevious"));
      DumbAwareAction findNextAction = DumbAwareAction.create(event -> {
        int selectedRow = myResultsPreviewTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < myResultsPreviewTable.getRowCount() - 1) {
          myResultsPreviewTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
          ScrollingUtil.ensureIndexIsVisible(myResultsPreviewTable, selectedRow + 1, 1);
        }
      });
      DumbAwareAction findPreviousAction = DumbAwareAction.create(event -> {
        int selectedRow = myResultsPreviewTable.getSelectedRow();
        if (selectedRow > 0 && selectedRow <= myResultsPreviewTable.getRowCount() - 1) {
          myResultsPreviewTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
          ScrollingUtil.ensureIndexIsVisible(myResultsPreviewTable, selectedRow - 1, 1);
        }
      });
      for (JComponent component : tableAware) {
        findNextAction.registerCustomShortcutSet(findNextShortcutSet, component);
        findPreviousAction.registerCustomShortcutSet(findPreviousShortcutSet, component);
      }
    }
    myUsagePreviewTitle = new SimpleColoredComponent();
    myUsagePreviewTitle.setBorder(JBUI.Borders.empty(3, 8, 4, 8));
    myUsageViewPresentation = new UsageViewPresentation();
    myUsagePreviewPanel = new UsagePreviewPanel(myProject, myUsageViewPresentation, Registry.is("ide.find.as.popup.editable.code")) {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(myResultsPreviewTable.getWidth(), Math.max(getHeight(), getLineHeight() * 15));
      }
    };
    Disposer.register(myDisposable, myUsagePreviewPanel);
    final Runnable updatePreviewRunnable = () -> {
      if (Disposer.isDisposed(myDisposable)) return;
      int[] selectedRows = myResultsPreviewTable.getSelectedRows();
      final List<UsageInfo> selection = new SmartList<>();
      String file = null;
      for (int row : selectedRows) {
        Object value = myResultsPreviewTable.getModel().getValueAt(row, 0);
        UsageInfoAdapter adapter = (UsageInfoAdapter) value;
        file = adapter.getPath();
        if (adapter.isValid()) {
          selection.addAll(Arrays.asList(adapter.getMergedInfos()));
        }
      }
      myReplaceSelectedButton.setText(FindBundle.message("find.popup.replace.selected.button", selection.size()));
      FindInProjectUtil.setupViewPresentation(myUsageViewPresentation, myHelper.getModel().clone());
      myUsagePreviewPanel.updateLayout(selection);
      myUsagePreviewTitle.clear();
      if (myUsagePreviewPanel.getCannotPreviewMessage(selection) == null && file != null) {
        myUsagePreviewTitle.append(PathUtil.getFileName(file), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(file), true);
        String locationPath = virtualFile == null ? null : getPresentablePath(myProject, virtualFile.getParent(), 120);
        if (locationPath != null) {
          myUsagePreviewTitle.append(spaceAndThinSpace() + locationPath,
                                     new SimpleTextAttributes(STYLE_PLAIN, UIUtil.getContextHelpForeground()));
        }
      }
    };
    myResultsPreviewTable.getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting() || Disposer.isDisposed(myPreviewUpdater)) return;
      myPreviewUpdater.addRequest(updatePreviewRunnable, 50); //todo[vasya]: remove this dirty hack of updating preview panel after clicking on Replace button
    });
    DocumentAdapter documentAdapter = new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        if (myDialog == null) return;
        if (e.getDocument() == mySearchComponent.getDocument()) {
          scheduleResultsUpdate();
        }
        if (e.getDocument() == myReplaceComponent.getDocument()) {
          applyTo(myHelper.getModel());
          if (myHelper.getModel().isRegularExpressions()) {
            myComponentValidator.updateInfo(getValidationInfo(myHelper.getModel()));
          }
          ApplicationManager.getApplication().invokeLater(updatePreviewRunnable);
        }
      }
    };
    mySearchComponent.getDocument().addDocumentListener(documentAdapter);
    myReplaceComponent.getDocument().addDocumentListener(documentAdapter);

    mySearchRescheduleOnCancellationsAlarm = new Alarm();

    JBSplitter splitter = new OnePixelSplitter(true, .33f);
    splitter.setSplitterProportionKey(SPLITTER_SERVICE_KEY);
    splitter.getDivider().setBackground(OnePixelDivider.BACKGROUND);
    JBScrollPane scrollPane = new JBScrollPane(myResultsPreviewTable) {
      @Override
      public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.height = myResultsPreviewTable.getPreferredScrollableViewportSize().height;
        return size;
      }
    };
    scrollPane.setBorder(JBUI.Borders.empty());
    splitter.setFirstComponent(scrollPane);
    JPanel bottomPanel = new JPanel(new MigLayout("flowx, ins 4 4 4 4, fillx, hidemode 3, gap 0"));
    bottomPanel.add(tabOptionsButton);
    myOKHintLabel = new JBLabel("");
    myOKHintLabel.setEnabled(false);
    myNavigationHintLabel = new JBLabel("");
    myNavigationHintLabel.setEnabled(false);
    myNavigationHintLabel.setFont(JBUI.Fonts.smallFont());
    Insets insets = myOKButton.getInsets();
    String btnGapLeft = "gapleft " + Math.max(0, JBUIScale.scale(12) - insets.left - insets.right);

    bottomPanel.add(myNavigationHintLabel, btnGapLeft);
    bottomPanel.add(Box.createHorizontalGlue(), "growx, pushx");
    bottomPanel.add(myOKHintLabel);
    bottomPanel.add(myOKButton, btnGapLeft);
    bottomPanel.add(myReplaceAllButton, btnGapLeft);
    bottomPanel.add(myReplaceSelectedButton, btnGapLeft);

    myCodePreviewComponent = myUsagePreviewPanel.createComponent();
    JPanel previewPanel = new JPanel(new BorderLayout());
    previewPanel.add(myUsagePreviewTitle, BorderLayout.NORTH);
    previewPanel.add(myCodePreviewComponent, BorderLayout.CENTER);
    splitter.setSecondComponent(previewPanel);
    JPanel scopesPanel = new JPanel(new MigLayout("flowx, gap 26, ins 0"));
    scopesPanel.add(myScopeSelectionToolbar.getComponent());
    scopesPanel.add(myScopeDetailsPanel, "growx, pushx");
    setLayout(new MigLayout("flowx, ins 4, gap 0, fillx, hidemode 3"));
    myTitlePanel = new JPanel(new MigLayout("flowx, ins 0, gap 0, fillx, filly"));
    myTitlePanel.add(myTitleLabel, "gapright 4");
    myTitlePanel.add(myInfoLabel);
    myTitlePanel.add(myLoadingDecorator.getComponent(), "w 24, wmin 24");
    myTitlePanel.add(Box.createHorizontalGlue(), "growx, pushx");

    add(myTitlePanel, "sx 2, growx, growx, growy");
    add(myCbFileFilter);
    add(myFileMaskField, "gapleft 4, gapright 16");
    if (Registry.is("ide.find.as.popup.allow.pin") || ApplicationManager.getApplication().isInternal()) {
      myIsPinned.set(UISettings.getInstance().getPinFindInPath());
      JPanel twoButtons = new JPanel(new MigLayout("flowx, ins 0, gap 4, fillx, hidemode 3"));
      twoButtons.add(myFilterContextButton);
      JComponent separatorComponent = (JComponent)Box.createRigidArea(new JBDimension(1, 24));
      separatorComponent.setOpaque(true);
      separatorComponent.setBackground(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground());
      twoButtons.add(separatorComponent);
      twoButtons.add(pinButton);
      add(twoButtons, "wrap");
    }
    else {
      add(myFilterContextButton, "wrap");
    }
    mySearchTextArea.setBorder(
      new CompoundBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.searchFieldBorderColor(), 1, 0, 1, 0),
                         JBUI.Borders.empty(1, 0, 2, 0)));
    add(mySearchTextArea, "pushx, growx, sx 10, pad 0 -4 0 4, gaptop 4, wrap");
    myReplaceTextArea.setBorder(
      new CompoundBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.searchFieldBorderColor(), 0, 0, 1, 0),
                         JBUI.Borders.empty(1, 0, 2, 0)));
    add(myReplaceTextArea, "pushx, growx, sx 10, pad 0 -4 0 4, wrap");
    add(scopesPanel, "sx 10, pushx, growx, ax left, wrap, gaptop 4, gapbottom 4");
    add(splitter, "pushx, growx, growy, pushy, sx 10, wrap, pad -4 -4 4 4");
    add(bottomPanel, "pushx, growx, dock south, sx 10");

    MnemonicHelper.init(this);

    List<Component> focusOrder = new ArrayList<>();
    focusOrder.add(mySearchComponent);
    focusOrder.add(myReplaceComponent);
    focusOrder.addAll(searchExtraButtons);
    focusOrder.addAll(replaceExtraButtons);
    focusOrder.add(myCbFileFilter);
    ContainerUtil.addAll(focusOrder, focusableComponents(myScopeDetailsPanel));
    focusOrder.add(editorComponent);
    ContainerUtil.addAll(focusOrder, focusableComponents(bottomPanel));
    setFocusCycleRoot(true);
    setFocusTraversalPolicy(new ListFocusTraversalPolicy(focusOrder));
  }

  @Contract("_,!null,_->!null")
  private static String getPresentablePath(@NotNull Project project, @Nullable VirtualFile virtualFile, int maxChars) {
    if (virtualFile == null) return null;
    String path = ScratchUtil.isScratch(virtualFile)
               ? ScratchUtil.getRelativePath(project, virtualFile)
               : VfsUtilCore.isAncestor(project.getBaseDir(), virtualFile, true)
                 ? VfsUtilCore.getRelativeLocation(virtualFile, project.getBaseDir())
                 : FileUtil.getLocationRelativeToUserHome(virtualFile.getPath());
    return path == null ? null : maxChars < 0 ? path : StringUtil.trimMiddle(path, maxChars);
  }

  private DefaultTableModel createTableModel() {
    final DefaultTableModel model = new DefaultTableModel() {
      private String firstResultPath;

      private final Comparator<Vector<UsageInfoAdapter>> COMPARATOR = (v1, v2) -> {
        UsageInfoAdapter u1 = v1.get(0);
        UsageInfoAdapter u2 = v2.get(0);
        String u2Path = u2.getPath();
        final String u1Path = u1.getPath();
        if (u1Path.equals(firstResultPath) && !u2Path.equals(firstResultPath)) return -1; // first result is always sorted first
        if (!u1Path.equals(firstResultPath) && u2Path.equals(firstResultPath)) return 1;
        int c = u1Path.compareTo(u2Path);
        if (c != 0) return c;
        c = Integer.compare(u1.getLine(), u2.getLine());
        if (c != 0) return c;
        return Integer.compare(u1.getNavigationOffset(), u2.getNavigationOffset());
      };

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }

      @SuppressWarnings({"UseOfObsoleteCollectionType", "unchecked", "rawtypes"})
      @Override
      //Inserts search results in sorted order
      public void addRow(Object[] rowData) {
        if (myNeedReset.compareAndSet(true, false)) {
          dataVector.clear();
          fireTableDataChanged();
        }
        final Vector<UsageInfoAdapter> v = (Vector)convertToVector(rowData);
        if (dataVector.isEmpty()) {
          addRow(v);
          myResultsPreviewTable.getSelectionModel().setSelectionInterval(0, 0);
          firstResultPath = v.get(0).getPath();
        }
        else {
          final int p = Collections.binarySearch((Vector<Vector<UsageInfoAdapter>>)dataVector, v, COMPARATOR);
          if (p < 0) {
            // only insert when not already present.
            int row = -(p + 1);
            insertRow(row, v);
          }
        }
      }
    };

    model.addColumn("Usages");
    return model;
  }

  private void processCtrlEnter() {
    if (Registry.is("ide.find.enter.as.ok", false)) {
      navigateToSelectedUsage(null);
    }
    else {
      myOkActionListener.actionPerformed(null);
    }
  }

  private void onFileMaskChanged() {
    Object item = myFileMaskField.getEditor().getItem();
    if (item != null && !item.equals(myFileMaskField.getSelectedItem())){
      myFileMaskField.setSelectedItem(item);
    }
    scheduleResultsUpdate();
  }

  private void closeImmediately() {
    if (canBeClosedImmediately() && myDialog != null && myDialog.isVisible()) {
      myIsPinned.set(false);
      myDialog.doCancelAction();
    }
  }

  //Some popups shown above may prevent panel closing, first of all we should close them
  private boolean canBeClosedImmediately() {
    boolean state = myIsPinned.get();
    myIsPinned.set(false);
    try {
      //Here we actually close popups
      return myDialog != null && canBeClosed();
    } finally {
      myIsPinned.set(state);
    }
  }

  private void doOK(boolean openInFindWindow) {
    if (!canBeClosedImmediately()) {
      return;
    }

    FindModel validateModel = myHelper.getModel().clone();
    applyTo(validateModel);

    ValidationInfo validationInfo = getValidationInfo(validateModel);

    if (validationInfo == null) {
      if (validateModel.isReplaceState() &&
          !openInFindWindow &&
          myResultsPreviewTable.getRowCount() > 1 &&
          !ReplaceInProjectManager.getInstance(myProject).showReplaceAllConfirmDialog(
            myUsagesCount,
            getStringToFind(),
            myFilesCount,
            getStringToReplace())) {
        return;
      }
      myHelper.getModel().copyFrom(validateModel);
      myHelper.getModel().setPromptOnReplace(openInFindWindow);
      myHelper.doOKAction();
    }
    else {
      String message = validationInfo.message;
      Messages.showMessageDialog(this, message, CommonBundle.getErrorTitle(), Messages.getErrorIcon());
      return;
    }
    myIsPinned.set(false);
    myDialog.doCancelAction();
  }

  @NotNull
  private static StateRestoringCheckBox createCheckBox(@PropertyKey(resourceBundle = FindBundle.BUNDLE) String messageKey, String optionName) {
    StateRestoringCheckBox checkBox = new StateRestoringCheckBox(FindBundle.message(messageKey));
    checkBox.addActionListener(
      __ -> FUCounterUsageLogger.getInstance().logEvent(
        "find", "check.box.toggled", new FeatureUsageData().
          addData("type", FIND_TYPE).
          addData("option_name", optionName).
          addData("option_value", checkBox.isSelected())
      )
    );
    return checkBox;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    ApplicationManager.getApplication().invokeLater(() -> ScrollingUtil.ensureSelectionExists(myResultsPreviewTable), ModalityState.any());
    myScopeSelectionToolbar.updateActionsImmediately();
  }

  @Override
  public void initByModel() {
    FindModel myModel = myHelper.getModel();
    myCaseSensitiveState.set(myModel.isCaseSensitive());
    myWholeWordsState.set(myModel.isWholeWordsOnly());
    myRegexState.set(myModel.isRegularExpressions());

    mySelectedContextName = getSearchContextName(myModel);
    if (myModel.isReplaceState()) {
      myPreserveCaseState.set(myModel.isPreserveCase());
    }

    mySelectedScope = myScopeUI.initByModel(myModel);

    boolean isThereFileFilter = myModel.getFileFilter() != null && !myModel.getFileFilter().isEmpty();
    try {
      myCbFileFilter.putClientProperty("dontRequestFocus", Boolean.TRUE);
      myCbFileFilter.setSelected(isThereFileFilter);
    }
    finally {
      myCbFileFilter.putClientProperty("dontRequestFocus", null);
    }
    myFileMaskField.removeAllItems();
    List<String> variants = Arrays.asList(ArrayUtil.reverseArray(FindSettings.getInstance().getRecentFileMasks()));
    for (String variant : variants) {
      myFileMaskField.addItem(variant);
    }
    if (!variants.isEmpty()) {
      myFileMaskField.setSelectedItem(variants.get(0));
    }
    myFileMaskField.setEnabled(isThereFileFilter);
    String toSearch = myModel.getStringToFind();
    FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(myProject);

    if (StringUtil.isEmpty(toSearch)) {
      String[] history = findInProjectSettings.getRecentFindStrings();
      toSearch = history.length > 0 ? history[history.length - 1] : "";
    }

    mySearchComponent.setText(toSearch);
    String toReplace = myModel.getStringToReplace();

    if (StringUtil.isEmpty(toReplace)) {
      String[] history = findInProjectSettings.getRecentReplaceStrings();
      toReplace = history.length > 0 ? history[history.length - 1] : "";
    }
    myReplaceComponent.setText(toReplace);
    updateControls();
    updateScopeDetailsPanel();

    boolean isReplaceState = myHelper.isReplaceState();
    myTitleLabel.setText(myHelper.getTitle());
    myReplaceTextArea.setVisible(isReplaceState);
    if (Registry.is("ide.find.enter.as.ok", false)) {
      myOKHintLabel.setText(KeymapUtil.getKeystrokeText(ENTER));
    }
    else {
      myOKHintLabel.setText(KeymapUtil.getKeystrokeText(ENTER_WITH_MODIFIERS));
    }
    myOKButton.setText(FindBundle.message("find.popup.find.button"));
    myReplaceAllButton.setVisible(isReplaceState);
    myReplaceSelectedButton.setVisible(isReplaceState);
  }

  private void updateControls() {
    myReplaceAllButton.setVisible(myHelper.isReplaceState());
    myReplaceSelectedButton.setVisible(myHelper.isReplaceState());
    myNavigationHintLabel.setVisible(mySearchComponent.getText().contains("\n"));
    mySearchTextArea.updateExtraActions();
    myReplaceTextArea.updateExtraActions();
    if (myNavigationHintLabel.isVisible()) {
      myNavigationHintLabel.setText("");
      KeymapManager keymapManager = KeymapManager.getInstance();
      Keymap activeKeymap = keymapManager != null ? keymapManager.getActiveKeymap() : null;
      if (activeKeymap != null) {
        String findNextText = KeymapUtil.getFirstKeyboardShortcutText("FindNext");
        String findPreviousText = KeymapUtil.getFirstKeyboardShortcutText("FindPrevious");
        if (!StringUtil.isEmpty(findNextText) &&  !StringUtil.isEmpty(findPreviousText)) {
          myNavigationHintLabel.setText(FindBundle.message("label.use.0.and.1.to.select.usages", findNextText, findPreviousText));
        }
      }
    }
  }

  private void updateScopeDetailsPanel() {
    ((CardLayout)myScopeDetailsPanel.getLayout()).show(myScopeDetailsPanel, mySelectedScope.name);
    Component firstFocusableComponent = focusableComponents(myScopeDetailsPanel).find(
      c -> c.isFocusable() && c.isEnabled() && c.isShowing()
    );
    myScopeDetailsPanel.revalidate();
    myScopeDetailsPanel.repaint();
    if (firstFocusableComponent != null) {
      ApplicationManager.getApplication().invokeLater(
        () -> IdeFocusManager.getInstance(myProject).requestFocus(firstFocusableComponent, true));
    }
    if (firstFocusableComponent == null && !mySearchComponent.isFocusOwner() && !myReplaceComponent.isFocusOwner()) {
      ApplicationManager.getApplication().invokeLater(
        () -> IdeFocusManager.getInstance(myProject).requestFocus(mySearchComponent, true));
    }
  }

  @SuppressWarnings("WeakerAccess")
  public void scheduleResultsUpdate() {
    if (myDialog == null || !myDialog.isVisible()) return;
    if (mySearchRescheduleOnCancellationsAlarm == null || mySearchRescheduleOnCancellationsAlarm.isDisposed()) return;
    updateControls();
    mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
    mySearchRescheduleOnCancellationsAlarm.addRequest(this::findSettingsChanged, 100);
  }

  private void finishPreviousPreviewSearch() {
    if (myResultsPreviewSearchProgress != null && !myResultsPreviewSearchProgress.isCanceled()) {
      myResultsPreviewSearchProgress.cancel();
    }
  }

  private void findSettingsChanged() {
    if (isShowing()) {
      ScrollingUtil.ensureSelectionExists(myResultsPreviewTable);
    }
    final ModalityState state = ModalityState.current();
    finishPreviousPreviewSearch();
    mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
    applyTo(myHelper.getModel());
    FindModel findModel = new FindModel();
    findModel.copyFrom(myHelper.getModel());
    if (findModel.getStringToFind().contains("\n") && Registry.is("ide.find.ignores.leading.whitespace.in.multiline.search")) {
      findModel.setMultiline(true);
    }

    ValidationInfo result = getValidationInfo(myHelper.getModel());
    myComponentValidator.updateInfo(result);

    final ProgressIndicatorBase progressIndicatorWhenSearchStarted = new ProgressIndicatorBase() {
      @Override
      public void stop() {
        super.stop();
        onStop(System.identityHashCode(this));
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myNeedReset.compareAndSet(true, false)) { //nothing is found, let's clear previous results
            reset();
          }
        });
      }
    };
    myResultsPreviewSearchProgress = progressIndicatorWhenSearchStarted;
    final int hash = System.identityHashCode(myResultsPreviewSearchProgress);

    // Use previously shown usage files as hint for faster search and better usage preview performance if pattern length increased
    Set<VirtualFile> filesToScanInitially = new LinkedHashSet<>();

    if (myHelper.myPreviousModel != null && myHelper.myPreviousModel.getStringToFind().length() < myHelper.getModel().getStringToFind().length()) {
      final DefaultTableModel previousModel = (DefaultTableModel)myResultsPreviewTable.getModel();
      for (int i = 0, len = previousModel.getRowCount(); i < len; ++i) {
        final Object value = previousModel.getValueAt(i, 0);
        if (value instanceof UsageInfo2UsageAdapter) {
          final UsageInfo2UsageAdapter usage = (UsageInfo2UsageAdapter)value;
          final VirtualFile file = usage.getFile();
          if (file != null) filesToScanInitially.add(file);
        }
      }
    }

    myHelper.myPreviousModel = myHelper.getModel().clone();

    myReplaceAllButton.setEnabled(false);
    myReplaceSelectedButton.setEnabled(false);
    myReplaceSelectedButton.setText(FindBundle.message("find.popup.replace.selected.button", 0));

    onStart(hash);
    if (result != null && result.component != myReplaceComponent) {
      onStop(hash, result.message);
      reset();
      return;
    }

    FindInProjectExecutor projectExecutor = FindInProjectExecutor.Companion.getInstance();
    GlobalSearchScope scope = GlobalSearchScopeUtil.toGlobalSearchScope(
      FindInProjectUtil.getScopeFromModel(myProject, myHelper.myPreviousModel), myProject);
    TableCellRenderer renderer = projectExecutor.createTableCellRenderer();
    if (renderer == null) renderer = new UsageTableCellRenderer(scope);
    myResultsPreviewTable.getColumnModel().getColumn(0).setCellRenderer(renderer);

    final AtomicInteger resultsCount = new AtomicInteger();
    final AtomicInteger resultsFilesCount = new AtomicInteger();
    FindInProjectUtil.setupViewPresentation(myUsageViewPresentation, findModel);

    ProgressIndicatorUtils.scheduleWithWriteActionPriority(myResultsPreviewSearchProgress, new ReadTask() {
      @Override
      public Continuation performInReadAction(@NotNull ProgressIndicator indicator) {

        final FindUsagesProcessPresentation processPresentation =
          FindInProjectUtil.setupProcessPresentation(myProject, myUsageViewPresentation);
        ThreadLocal<String> lastUsageFileRef = new ThreadLocal<>();
        ThreadLocal<Reference<Usage>> recentUsageRef = new ThreadLocal<>();

        projectExecutor.findUsages(myProject, myResultsPreviewSearchProgress, processPresentation, findModel, filesToScanInitially, usage -> {
          if(isCancelled()) {
            onStop(hash);
            return false;
          }

          String file = lastUsageFileRef.get();
          String usageFile = PathUtil.toSystemIndependentName(usage.getPath());
          if (file == null || !file.equals(usageFile)) {
            resultsFilesCount.incrementAndGet();
            lastUsageFileRef.set(usageFile);
          }

          Usage recent = SoftReference.dereference(recentUsageRef.get());
          UsageInfoAdapter recentAdapter = recent instanceof UsageInfoAdapter ? (UsageInfoAdapter)recent : null;
          final boolean merged = !myHelper.isReplaceState() && recentAdapter != null && recentAdapter.merge(usage);
          if (!merged) {
            recentUsageRef.set(new WeakReference<>(usage));
          }

          ApplicationManager.getApplication().invokeLater(() -> {
            if (isCancelled()) {
              onStop(hash);
              return;
            }
            DefaultTableModel model = (DefaultTableModel)myResultsPreviewTable.getModel();
            if (!merged) {
              model.addRow(new Object[]{usage});
            }
            else {
              model.fireTableRowsUpdated(model.getRowCount() - 1, model.getRowCount() - 1);
            }
            myCodePreviewComponent.setVisible(true);
            if (model.getRowCount() == 1) {
              myResultsPreviewTable.setRowSelectionInterval(0, 0);
            }
            int occurrences = resultsCount.get();
            int filesWithOccurrences = resultsFilesCount.get();
            myCodePreviewComponent.setVisible(occurrences > 0);
            myReplaceAllButton.setEnabled(occurrences > 0);
            myReplaceSelectedButton.setEnabled(occurrences > 0);

            StringBuilder stringBuilder = new StringBuilder();
            if (occurrences > 0) {
              stringBuilder.append(Math.min(ShowUsagesAction.getUsagesPageSize(), occurrences));
              boolean foundAllUsages = occurrences < ShowUsagesAction.getUsagesPageSize();
              myUsagesCount = String.valueOf(occurrences);
              if (!foundAllUsages) {
                stringBuilder.append("+");
                myUsagesCount += "+";
              }
              stringBuilder.append(UIBundle.message("message.matches", occurrences));
              stringBuilder.append(" in ");
              stringBuilder.append(filesWithOccurrences);
              myFilesCount = String.valueOf(filesWithOccurrences);
              if (!foundAllUsages) {
                stringBuilder.append("+");
                myFilesCount += "+";
              }
              stringBuilder.append(UIBundle.message("message.files", filesWithOccurrences));
            }
            myInfoLabel.setText(stringBuilder.toString());
          }, state);

          boolean continueSearch = resultsCount.incrementAndGet() < ShowUsagesAction.getUsagesPageSize();
          if (!continueSearch) {
            onStop(hash);
          }
          return continueSearch;
        });

        return new Continuation(() -> {
          if (!isCancelled()) {
            if (resultsCount.get() == 0) {
              showEmptyText(null);
            }
          }
          onStop(hash);
        }, state);
      }

      boolean isCancelled() {
        return progressIndicatorWhenSearchStarted != myResultsPreviewSearchProgress || progressIndicatorWhenSearchStarted.isCanceled();
      }

      @Override
      public void onCanceled(@NotNull ProgressIndicator indicator) {
        if (isShowing() && progressIndicatorWhenSearchStarted == myResultsPreviewSearchProgress) {
          scheduleResultsUpdate();
        }
      }
    });
  }

  private void reset() {
    ((DefaultTableModel)myResultsPreviewTable.getModel()).getDataVector().clear();
    ((DefaultTableModel)myResultsPreviewTable.getModel()).fireTableDataChanged();
    myResultsPreviewTable.getSelectionModel().clearSelection();
    myInfoLabel.setText(null);
  }

  private void showEmptyText(@Nullable String message) {
    StatusText emptyText = myResultsPreviewTable.getEmptyText();
    emptyText.clear();
    emptyText.setText(message != null ? FindBundle.message("message.nothingFound.with.problem", message)
                                                                 : FindBundle.message("message.nothingFound"));
    FindModel model = myHelper.getModel();
    if (mySelectedScope == FindPopupScopeUIImpl.DIRECTORY && !model.isWithSubdirectories()) {
      emptyText.appendSecondaryText(FindBundle.message("find.recursively.hint"),
                                                               SimpleTextAttributes.LINK_ATTRIBUTES,
                                    e -> {
                                      model.setWithSubdirectories(true);
                                      scheduleResultsUpdate();
                                    });
    }
    List<Object> usedOptions = new SmartList<>();
    if (model.isCaseSensitive() && isEnabled(myCaseSensitiveAction)) {
      usedOptions.add(myCaseSensitiveAction);
    }
    if (model.isWholeWordsOnly() && isEnabled(myWholeWordsAction)) {
      usedOptions.add(myWholeWordsAction);
    }
    if (model.isRegularExpressions() && isEnabled(myRegexAction)) {
      usedOptions.add(myRegexAction);
    }
    boolean couldBeRegexp = false;
    if (mySuggestRegexHintForEmptyResults) {
      String stringToFind = model.getStringToFind();
      if (!model.isRegularExpressions() && isEnabled(myRegexAction)) {
        String regexSymbols = ".$|()[]{}^?*+\\";
        for (int i = 0; i < stringToFind.length(); i++) {
          if (regexSymbols.indexOf(stringToFind.charAt(i)) != -1) {
            couldBeRegexp = true;
            break;
          }
        }
      }
      if (couldBeRegexp) {
        try {
          Pattern.compile(stringToFind);
          usedOptions.add(myRegexAction);
        }
        catch (Exception e) {
          couldBeRegexp = false;
        }
      }
    }
    String fileTypeMask = getFileTypeMask();
    if (fileTypeMask != null && (FindInProjectUtil.createFileMaskCondition(fileTypeMask) != Conditions.<CharSequence>alwaysTrue())) {
      usedOptions.add(myCbFileFilter);
    }
    if (model.isInCommentsOnly()
    || model.isInStringLiteralsOnly()
        || model.isExceptComments()
    || model.isExceptStringLiterals()
    || model.isExceptCommentsAndStringLiterals()
    ) {
      usedOptions.add(model);
    }
    if (!usedOptions.isEmpty()) {
      emptyText.appendLine(" ");
      if (couldBeRegexp) {
        emptyText.appendLine(FindBundle.message("message.nothingFound.search.with.regex"), LINK_PLAIN_ATTRIBUTES, new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            toggleOption(myRegexAction);
            mySuggestRegexHintForEmptyResults = false;
          }
        }).appendText(" "+KeymapUtil.getFirstKeyboardShortcutText((myRegexAction).getShortcutSet()));
      }
      else if (usedOptions.size() > 1) {
        emptyText.appendLine(FindBundle.message("message.nothingFound.used.options"));
        StringBuilder sb = new StringBuilder();
        for (Object option : usedOptions) {
          if (sb.length() > 0) sb.append("  ");
          String optionText = getOptionText(option, true);
          if (optionText == null) continue;
          sb.append(optionText);
        }
        emptyText.appendLine(sb.toString());
        emptyText.appendLine(" ");
        emptyText.appendLine(FindBundle.message("message.nothingFound.clearAll"), LINK_PLAIN_ATTRIBUTES, new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            resetAllFilters();
          }
        }).appendText(" "+getOptionText(myResetFiltersAction, true));
      } else {
        Object option = usedOptions.get(0);
        emptyText.appendLine(FindBundle.message("message.nothingFound.used.option", getOptionText(option, false)));
        emptyText.appendLine(" ");
        emptyText.appendLine(FindBundle.message("message.nothingFound.clearOption"), LINK_PLAIN_ATTRIBUTES, new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            resetAllFilters();
          }
        }).appendText(" " + getOptionText(myResetFiltersAction, true));
      }
    } else {
      emptyText.appendLine(" ");
      emptyText.appendLine(FindBundle.message("message.nothingFound.default.hint"));
    }
  }

  private void resetAllFilters() {
    myCbFileFilter.setSelected(false);
    mySelectedContextName = FindInProjectUtil.getPresentableName(FindModel.SearchContext.ANY);
    myFilterContextButton.repaint();
    myCaseSensitiveState.set(false);
    myWholeWordsState.set(false);
    myRegexState.set(false);
    scheduleResultsUpdate();
  }

  private boolean isEnabled(AnAction action) {
    Presentation presentation = new Presentation();
    action.update(new AnActionEvent(null, DataManager.getInstance().getDataContext(this), ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), 0));
    return presentation.isEnabled();
  }

  @Nullable
  private static String getOptionText(Object option, boolean full) {
    if (option instanceof AnAction) {
      String text = ((AnAction)option).getTemplateText();
      if (text == null) text = "";
      return (text + (full ? " " + KeymapUtil.getFirstKeyboardShortcutText(((AnAction)option).getShortcutSet()) : "")).trim();
    }
    if (option instanceof JToggleButton) {
      CustomShortcutSet shortcutSet = KeymapUtil.getMnemonicAsShortcut(((JToggleButton)option).getMnemonic());
      return (((JToggleButton)option).getText().replace(":", "") +
             (shortcutSet != null && full ? " " + KeymapUtil.getFirstKeyboardShortcutText(shortcutSet) : "")).trim();
    }
    if (option instanceof FindModel) return FindBundle.message("message.nothingFound.context.filter");
    return null;
  }

  private void toggleOption(Object option) {
    if (option instanceof AnAction) {
      ((AnAction)option).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(this), ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
    } else if (option instanceof JToggleButton) {
      ((JToggleButton)option).doClick();
    } else if (option instanceof FindModel) {
      mySelectedContextName = FindInProjectUtil.getPresentableName(FindModel.SearchContext.ANY);
      myFilterContextButton.repaint();
      scheduleResultsUpdate();
    }
  }

  private void onStart(int hash) {
    myNeedReset.set(true);
    myLoadingHash = hash;
    myLoadingDecorator.startLoading(false);
    myResultsPreviewTable.getEmptyText().setText(FindBundle.message("empty.text.searching"));
  }


  private void onStop(int hash) {
    onStop(hash, null);
  }

  private void onStop(int hash, String message) {
    if (hash != myLoadingHash) {
      return;
    }
    UIUtil.invokeLaterIfNeeded(() -> {
      showEmptyText(message);
      myLoadingDecorator.stopLoading();
    });
  }

  @Override
  @Nullable
  public String getFileTypeMask() {
    String mask = null;
    if (myCbFileFilter != null && myCbFileFilter.isSelected()) {
      mask = (String)myFileMaskField.getSelectedItem();
    }
    return mask;
  }

  @Nullable("null means OK")
  private ValidationInfo getValidationInfo(@NotNull FindModel model) {
    ValidationInfo scopeValidationInfo = myScopeUI.validate(model, mySelectedScope);
    if (scopeValidationInfo != null) {
      return scopeValidationInfo;
    }

    if (!myHelper.canSearchThisString()) {
      return new ValidationInfo(FindBundle.message("find.empty.search.text.error"), mySearchComponent);
    }

    if (model.isRegularExpressions()) {
      String toFind = model.getStringToFind();
      Pattern pattern;
      try {
        pattern = Pattern.compile(toFind, model.isCaseSensitive() ? Pattern.MULTILINE : Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        if (pattern.matcher("").matches() && !toFind.endsWith("$") && !toFind.startsWith("^")) {
          return new ValidationInfo(FindBundle.message("find.empty.match.regular.expression.error"), mySearchComponent);
        }
      }
      catch (PatternSyntaxException e) {
        return new ValidationInfo(FindBundle.message("find.invalid.regular.expression.error", toFind, e.getDescription()),
                                  mySearchComponent);
      }
      if (model.isReplaceState()) {
        if (myResultsPreviewTable.getRowCount() > 0) {
          Object value = myResultsPreviewTable.getModel().getValueAt(0, 0);
          if (value instanceof Usage) {
            try {
              // Just check
              ReplaceInProjectManager.getInstance(myProject).replaceUsage((Usage)value, model, Collections.emptySet(), true);
            }
            catch (FindManager.MalformedReplacementStringException e) {
              return new ValidationInfo(e.getMessage(), myReplaceComponent);
            }
          }
        }

        try {
          RegExReplacementBuilder.validate(pattern, getStringToReplace());
        }
        catch (IllegalArgumentException e) {
          return new ValidationInfo(FindBundle.message("find.replace.invalid.replacement.string", e.getMessage()),
                                    myReplaceComponent);
        }
      }
    }

    final String mask = getFileTypeMask();

    if (mask != null) {
      if (mask.isEmpty()) {
        return new ValidationInfo(FindBundle.message("find.filter.empty.file.mask.error"), myFileMaskField);
      }

      if (mask.contains(";")) {
        return new ValidationInfo(FindBundle.message("message.file.masks.should.be.comma.separated"), myFileMaskField);
      }
      try {
        createFileMaskRegExp(mask);   // verify that the regexp compiles
      }
      catch (PatternSyntaxException ex) {
        return new ValidationInfo(FindBundle.message("find.filter.invalid.file.mask.error", mask), myFileMaskField);
      }
    }
    return null;
  }

  @Override
  @NotNull
  public String getStringToFind() {
    return mySearchComponent.getText();
  }

  @NotNull
  private String getStringToReplace() {
    return myReplaceComponent.getText();
  }

  private void applyTo(@NotNull FindModel model) {
    model.setCaseSensitive(myCaseSensitiveState.get());
    if (model.isReplaceState()) {
      model.setPreserveCase(myPreserveCaseState.get());
    }
    model.setWholeWordsOnly(myWholeWordsState.get());

    String selectedSearchContextInUi = mySelectedContextName;
    FindModel.SearchContext searchContext = parseSearchContext(selectedSearchContextInUi);

    model.setSearchContext(searchContext);
    model.setRegularExpressions(myRegexState.get());
    model.setStringToFind(getStringToFind());

    if (model.isReplaceState()) {
      model.setStringToReplace(StringUtil.convertLineSeparators(getStringToReplace()));
    }

    model.setProjectScope(false);
    model.setDirectoryName(null);
    model.setModuleName(null);
    model.setCustomScopeName(null);
    model.setCustomScope(null);
    model.setCustomScope(false);
    myScopeUI.applyTo(model, mySelectedScope);

    model.setFindAll(false);

    String mask = getFileTypeMask();
    model.setFileFilter(mask);
  }

  private void navigateToSelectedUsage(@Nullable AnActionEvent e) {
    Navigatable[] navigatables = e != null ? e.getData(CommonDataKeys.NAVIGATABLE_ARRAY) : null;
    if (navigatables != null) {
      if (canBeClosed()) {
        myDialog.doCancelAction();
      }
      OpenSourceUtil.navigate(navigatables);
      return;
    }

    Map<Integer, Usage> usages = getSelectedUsages();
    if (usages != null) {
      if (canBeClosed()) {
        myDialog.doCancelAction();
      }
      boolean first = true;
      for (Usage usage : usages.values()) {
        if (first) {
          usage.navigate(true);
        }
        else {
          usage.highlightInEditor();
        }
        first = false;
      }
    }
  }

  @Nullable
  private Map<Integer, Usage> getSelectedUsages() {
    int[] rows = myResultsPreviewTable.getSelectedRows();
    Map<Integer, Usage> result = null;
    for (int i = rows.length - 1; i >= 0; i--) {
      int row = rows[i];
      Object valueAt = myResultsPreviewTable.getModel().getValueAt(row, 0);
      if (valueAt instanceof Usage) {
        if (result == null) result = new LinkedHashMap<>();
        result.put(row, (Usage)valueAt);
      }
    }
    return result;
  }

  public static ActionToolbarImpl createToolbar(AnAction... actions) {
    ActionToolbarImpl toolbar = (ActionToolbarImpl)ActionManager.getInstance()
      .createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, new DefaultActionGroup(actions), true);
    toolbar.setForceMinimumSize(true);
    toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    return toolbar;
  }

  private static void applyFont(JBFont font, Component... components) {
    for (Component component : components) {
      component.setFont(font);
    }
  }

  private static void createFileMaskRegExp(@Nullable String filter) throws PatternSyntaxException {
    if (filter == null) {
      return;
    }
    String pattern;
    final List<String> strings = StringUtil.split(filter, ",");
    if (strings.size() == 1) {
      pattern = PatternUtil.convertToRegex(filter.trim());
    }
    else {
      pattern = StringUtil.join(strings, s -> "(" + PatternUtil.convertToRegex(s.trim()) + ")", "|");
    }
    // just check validity
    //noinspection ResultOfMethodCallIgnored
    Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
  }

  @NotNull
  private static String getSearchContextName(FindModel model) {
    String searchContext = FindBundle.message("find.context.anywhere.scope.label");
    if (model.isInCommentsOnly()) searchContext = FindBundle.message("find.context.in.comments.scope.label");
    else if (model.isInStringLiteralsOnly()) searchContext = FindBundle.message("find.context.in.literals.scope.label");
    else if (model.isExceptStringLiterals()) searchContext = FindBundle.message("find.context.except.literals.scope.label");
    else if (model.isExceptComments()) searchContext = FindBundle.message("find.context.except.comments.scope.label");
    else if (model.isExceptCommentsAndStringLiterals()) searchContext = FindBundle.message("find.context.except.comments.and.literals.scope.label");
    return searchContext;
  }

  @NotNull
  private static FindModel.SearchContext parseSearchContext(String presentableName) {
    FindModel.SearchContext searchContext = FindModel.SearchContext.ANY;
    if (FindBundle.message("find.context.in.literals.scope.label").equals(presentableName)) {
      searchContext = FindModel.SearchContext.IN_STRING_LITERALS;
    }
    else if (FindBundle.message("find.context.in.comments.scope.label").equals(presentableName)) {
      searchContext = FindModel.SearchContext.IN_COMMENTS;
    }
    else if (FindBundle.message("find.context.except.comments.scope.label").equals(presentableName)) {
      searchContext = FindModel.SearchContext.EXCEPT_COMMENTS;
    }
    else if (FindBundle.message("find.context.except.literals.scope.label").equals(presentableName)) {
      searchContext = FindModel.SearchContext.EXCEPT_STRING_LITERALS;
    }
    else if (FindBundle.message("find.context.except.comments.and.literals.scope.label").equals(presentableName)) {
      searchContext = FindModel.SearchContext.EXCEPT_COMMENTS_AND_STRING_LITERALS;
    }
    return searchContext;
  }

  @NotNull
  private static JBIterable<Component> focusableComponents(@NotNull Component component) {
    return UIUtil.uiTraverser(component)
      .bfsTraversal()
      .filter(c -> c instanceof JComboBox || c instanceof AbstractButton || c instanceof JTextComponent);
  }

  private class MySwitchStateToggleAction extends DumbAwareToggleAction implements TooltipLinkProvider {
    private final String myOptionName;
    private final AtomicBoolean myState;
    private final Producer<Boolean> myEnableStateProvider;
    private final Pair<@NotNull String, @NotNull Runnable> myTooltipLink;

    private MySwitchStateToggleAction(String message,
                                      String optionName,
                                      Icon icon, Icon hoveredIcon, Icon selectedIcon,
                                      AtomicBoolean state,
                                      Producer<Boolean> enableStateProvider) {
      this(message, optionName, icon, hoveredIcon, selectedIcon, state, enableStateProvider, null);
    }

    private MySwitchStateToggleAction(String message,
                                      String optionName,
                                      Icon icon, Icon hoveredIcon, Icon selectedIcon,
                                      AtomicBoolean state,
                                      Producer<Boolean> enableStateProvider,
                                      @Nullable Pair<@NotNull String, @NotNull Runnable> tooltipLink) {
      super(FindBundle.message(message), null, icon);
      myOptionName = optionName;
      myState = state;
      myEnableStateProvider = enableStateProvider;
      myTooltipLink = tooltipLink;
      getTemplatePresentation().setHoveredIcon(hoveredIcon);
      getTemplatePresentation().setSelectedIcon(selectedIcon);
      ShortcutSet shortcut = ActionUtil.getMnemonicAsShortcut(this);
      if (shortcut != null) {
        setShortcutSet(shortcut);
        registerCustomShortcutSet(shortcut, FindPopupPanel.this);
      }
    }

    @Override
    public @Nullable Pair<@NotNull String, @NotNull Runnable> getTooltipLink(@Nullable JComponent owner) {
      return myTooltipLink;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return myState.get();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(myEnableStateProvider.produce());
      Toggleable.setSelected(e.getPresentation(), myState.get());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
      FUCounterUsageLogger.getInstance().logEvent(
        "find", "check.box.toggled", new FeatureUsageData().
          addData("type", FIND_TYPE).
          addData("option_name", myOptionName).
          addData("option_value", selected)
      );
      myState.set(selected);
      if (myState == myRegexState) {
        mySuggestRegexHintForEmptyResults = false;
      }
      scheduleResultsUpdate();
    }
  }

  private static class MyOpenResultsInNewTabAction extends ToggleAction {
    private MyOpenResultsInNewTabAction() {
      super(FindBundle.messagePointer("find.open.in.new.tab.action"));
    }

    @Override
    public boolean isDumbAware() {
      return true;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return FindSettings.getInstance().isShowResultsInSeparateView();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      FindSettings.getInstance().setShowResultsInSeparateView(state);
    }
  }

  private class MySwitchContextToggleAction extends ToggleAction implements DumbAware {
    MySwitchContextToggleAction(FindModel.SearchContext context) {
      super(FindInProjectUtil.getPresentableName(context));
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return Objects.equals(mySelectedContextName, getTemplatePresentation().getText());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      if (state) {
        mySelectedContextName = getTemplatePresentation().getText();
        myFilterContextButton.repaint();
        scheduleResultsUpdate();
      }
    }
  }

  private class MySelectScopeToggleAction extends DumbAwareToggleAction {
    private final FindPopupScopeUI.ScopeType myScope;

    MySelectScopeToggleAction(FindPopupScopeUI.ScopeType scope) {
      super(scope.textComputable.get(), null, scope.icon);
      getTemplatePresentation().setHoveredIcon(scope.icon);
      getTemplatePresentation().setDisabledIcon(scope.icon);
      myScope = scope;
    }

    @Override
    public boolean displayTextInToolbar() {
      return true;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return mySelectedScope == myScope;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      if (state) {
        mySelectedScope = myScope;
        myScopeSelectionToolbar.updateActionsImmediately();
        updateScopeDetailsPanel();
        scheduleResultsUpdate();
      }
    }
  }

  private class MyShowFilterPopupAction extends DumbAwareAction {
    private final PopupState myPopupState = new PopupState();
    private final DefaultActionGroup mySwitchContextGroup;

    MyShowFilterPopupAction() {
      super(FindBundle.messagePointer("find.popup.show.filter.popup"), Presentation.NULL_STRING, AllIcons.General.Filter);
      KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
      if (keyboardShortcut != null) {
        setShortcutSet(new CustomShortcutSet(keyboardShortcut));
      }
      mySwitchContextGroup = new DefaultActionGroup();
      mySwitchContextGroup.add(new MySwitchContextToggleAction(FindModel.SearchContext.ANY));
      mySwitchContextGroup.add(new MySwitchContextToggleAction(FindModel.SearchContext.IN_COMMENTS));
      mySwitchContextGroup.add(new MySwitchContextToggleAction(FindModel.SearchContext.IN_STRING_LITERALS));
      mySwitchContextGroup.add(new MySwitchContextToggleAction(FindModel.SearchContext.EXCEPT_COMMENTS));
      mySwitchContextGroup.add(new MySwitchContextToggleAction(FindModel.SearchContext.EXCEPT_STRING_LITERALS));
      mySwitchContextGroup.add(new MySwitchContextToggleAction(FindModel.SearchContext.EXCEPT_COMMENTS_AND_STRING_LITERALS));
      mySwitchContextGroup.setPopup(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (e.getData(PlatformDataKeys.CONTEXT_COMPONENT) == null) return;
      if (myPopupState.isRecentlyHidden()) return;

      ListPopup listPopup =
        JBPopupFactory.getInstance().createActionGroupPopup(null, mySwitchContextGroup, e.getDataContext(), false, null, 10);
      listPopup.addListener(myPopupState);
      listPopup.showUnderneathOf(myFilterContextButton);
    }
  }
  static class UsageTableCellRenderer extends JPanel implements TableCellRenderer {
    private final ColoredTableCellRenderer myUsageRenderer = new ColoredTableCellRenderer() {
      @Override
      protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
        if (value instanceof UsageInfo2UsageAdapter) {
          if (!((UsageInfo2UsageAdapter)value).isValid()) {
            myUsageRenderer.append(" " + UsageViewBundle.message("node.invalid") + " ", SimpleTextAttributes.ERROR_ATTRIBUTES);
          }
          TextChunk[] text = ((UsageInfo2UsageAdapter)value).getPresentation().getText();

          // skip line number / file info
          for (int i = 1; i < text.length; ++i) {
            TextChunk textChunk = text[i];
            SimpleTextAttributes attributes = getAttributes(textChunk);
            myUsageRenderer.append(textChunk.getText(), attributes);
          }
        }
        setBorder(null);
      }

      @NotNull
      private SimpleTextAttributes getAttributes(@NotNull TextChunk textChunk) {
        SimpleTextAttributes at = textChunk.getSimpleAttributesIgnoreBackground();
        boolean highlighted = textChunk.getType() != null || at.getFontStyle() == Font.BOLD;
        return highlighted
               ? new SimpleTextAttributes(null, at.getFgColor(), at.getWaveColor(),
                                          at.getStyle() & ~SimpleTextAttributes.STYLE_BOLD |
                                          SimpleTextAttributes.STYLE_SEARCH_MATCH)
               : at;
      }
    };

    private final ColoredTableCellRenderer myFileAndLineNumber = new ColoredTableCellRenderer() {
      private final SimpleTextAttributes REPEATED_FILE_ATTRIBUTES = new SimpleTextAttributes(STYLE_PLAIN, new JBColor(0xCCCCCC, 0x5E5E5E));
      private final SimpleTextAttributes ORDINAL_ATTRIBUTES = new SimpleTextAttributes(STYLE_PLAIN, new JBColor(0x999999, 0x999999));

      @Override
      protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
        if (value instanceof UsageInfo2UsageAdapter) {
          UsageInfo2UsageAdapter usageAdapter = (UsageInfo2UsageAdapter)value;
          TextChunk[] text = usageAdapter.getPresentation().getText();
          // line number / file info
          VirtualFile file = usageAdapter.getFile();
          String uniqueVirtualFilePath = getFilePath(usageAdapter);
          VirtualFile prevFile = findPrevFile(table, row, column);
          SimpleTextAttributes attributes = Comparing.equal(file, prevFile) ? REPEATED_FILE_ATTRIBUTES : ORDINAL_ATTRIBUTES;
          append(uniqueVirtualFilePath, attributes);
          if (text.length > 0) append(" " + text[0].getText(), ORDINAL_ATTRIBUTES);
        }
        setBorder(null);
      }

      @NotNull
      private String getFilePath(@NotNull UsageInfo2UsageAdapter ua) {
        VirtualFile file = ua.getFile();
        if (ScratchUtil.isScratch(file)) {
          return StringUtil.notNullize(getPresentablePath(ua.getUsageInfo().getProject(), ua.getFile(), 60));
        }
        return UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(ua.getUsageInfo().getProject(), file, myScope);
      }

      @Nullable
      private VirtualFile findPrevFile(@NotNull JTable table, int row, int column) {
        if (row <= 0) return null;
        Object prev = table.getValueAt(row - 1, column);
        return prev instanceof UsageInfo2UsageAdapter ? ((UsageInfo2UsageAdapter)prev).getFile() : null;
      }
    };

    private static final int MARGIN = 2;
    private final GlobalSearchScope myScope;

    UsageTableCellRenderer(GlobalSearchScope scope) {
      myScope = scope;
      setLayout(new BorderLayout());
      add(myUsageRenderer, BorderLayout.CENTER);
      add(myFileAndLineNumber, BorderLayout.EAST);
      setBorder(JBUI.Borders.empty(MARGIN, MARGIN, MARGIN, 0));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      myUsageRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      myFileAndLineNumber.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBackground(myUsageRenderer.getBackground());
      if (!isSelected && value instanceof UsageInfo2UsageAdapter) {
        UsageInfo2UsageAdapter usageAdapter = (UsageInfo2UsageAdapter)value;
        Project project = usageAdapter.getUsageInfo().getProject();
        Color color = VfsPresentationUtil.getFileBackgroundColor(project, usageAdapter.getFile());
        setBackground(color);
        myUsageRenderer.setBackground(color);
        myFileAndLineNumber.setBackground(color);
      }
      getAccessibleContext().setAccessibleName(FindBundle.message("find.popup.found.element.accesible.name", myUsageRenderer.getAccessibleContext().getAccessibleName(), myFileAndLineNumber.getAccessibleContext().getAccessibleName()));
      return this;
    }
  }

  private class MyPinAction extends ToggleAction {
    private MyPinAction() {super(IdeBundle.messagePointer("action.ToggleAction.text.pin.window"),
                                 IdeBundle.messagePointer("action.ToggleAction.description.pin.window"), AllIcons.General.Pin_tab);}

    @Override
    public boolean isDumbAware() {
      return true;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return UISettings.getInstance().getPinFindInPath();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      myIsPinned.set(state);
      UISettings.getInstance().setPinFindInPath(state);
      FUCounterUsageLogger.getInstance().logEvent(
        "find", "pin.toggled", new FeatureUsageData().
          addData("option_value", state)
      );
    }
  }

  private class MySkipTabWithOneUsageAction extends ToggleAction {
    private MySkipTabWithOneUsageAction() {
      super(FindBundle.messagePointer("find.options.skip.results.tab.with.one.usage.action"));
    }

    @Override
    public boolean isDumbAware() {
      return true;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return FindSettings.getInstance().isSkipResultsWithOneUsage();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      FindSettings.getInstance().setSkipResultsWithOneUsage(state);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setVisible(!myHelper.isReplaceState());
    }
  }

  private class MyEnterAction extends DumbAwareAction {
    private final boolean myEnterAsOK;

    private MyEnterAction(boolean enterAsOK) {
      myEnterAsOK = enterAsOK;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(
        e.getData(CommonDataKeys.EDITOR) == null ||
        SwingUtilities.isDescendingFrom(e.getData(PlatformDataKeys.CONTEXT_COMPONENT), myFileMaskField));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (SwingUtilities.isDescendingFrom(e.getData(PlatformDataKeys.CONTEXT_COMPONENT), myFileMaskField) && myFileMaskField.isPopupVisible()) {
        myFileMaskField.hidePopup();
        return;
      }
      if (myScopeUI.hideAllPopups()) {
        return;
      }
      if (myEnterAsOK) {
        myOkActionListener.actionPerformed(null);
      }
      else if (myHelper.isReplaceState()) {
        myReplaceSelectedButton.doClick();
      }
      else {
        navigateToSelectedUsage(null);
      }
    }
  }
}
