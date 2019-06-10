// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.find.editorHeaderActions.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.EditorHeaderComponent;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.BooleanGetter;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.LightColors;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.ui.mac.TouchbarDataKeys;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.List;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.META_DOWN_MASK;

public class SearchReplaceComponent extends EditorHeaderComponent implements DataProvider {
  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  private final MyTextComponentWrapper mySearchFieldWrapper;
  private JTextComponent mySearchTextComponent;

  private final MyTextComponentWrapper myReplaceFieldWrapper;
  private JTextComponent myReplaceTextComponent;

  private final JPanel myLeftPanel;
  private final JPanel myRightPanel;

  private final DefaultActionGroup mySearchFieldActions;
  private final ActionToolbarImpl mySearchActionsToolbar1;
  private final ActionToolbarImpl mySearchActionsToolbar2;
  @NotNull
  private final ActionToolbarImpl.PopupStateModifier mySearchToolbar1PopupStateModifier;

  private final DefaultActionGroup myReplaceFieldActions;
  private final ActionToolbarImpl myReplaceActionsToolbar1;
  private final ActionToolbarImpl myReplaceActionsToolbar2;
  private final JPanel myReplaceToolbarWrapper;

  private final Project myProject;
  private final JComponent myTargetComponent;

  private final Runnable myCloseAction;
  private final Runnable myReplaceAction;

  private final DataProvider myDataProviderDelegate;

  private boolean myMultilineMode;
  private String myStatusText = "";
  private DefaultActionGroup myTouchbarActions;

  @NotNull
  public static Builder buildFor(@Nullable Project project, @NotNull JComponent component) {
    return new Builder(project, component);
  }

  private SearchReplaceComponent(@Nullable Project project,
                                 @NotNull JComponent targetComponent,
                                 @NotNull DefaultActionGroup searchToolbar1Actions,
                                 @NotNull final BooleanGetter searchToolbar1ModifiedFlagGetter,
                                 @NotNull DefaultActionGroup searchToolbar2Actions,
                                 @NotNull DefaultActionGroup searchFieldActions,
                                 @NotNull DefaultActionGroup replaceToolbar1Actions,
                                 @NotNull DefaultActionGroup replaceToolbar2Actions,
                                 @NotNull DefaultActionGroup replaceFieldActions,
                                 @Nullable Runnable replaceAction,
                                 @Nullable Runnable closeAction,
                                 @Nullable DataProvider dataProvider) {
    myProject = project;
    myTargetComponent = targetComponent;
    mySearchFieldActions = searchFieldActions;
    myReplaceFieldActions = replaceFieldActions;
    myReplaceAction = replaceAction;
    myCloseAction = closeAction;

    mySearchToolbar1PopupStateModifier = new ActionToolbarImpl.PopupStateModifier() {
      @Override
      public int getModifiedPopupState() {
        return ActionButtonComponent.PUSHED;
      }

      @Override
      public boolean willModify() {
        return searchToolbar1ModifiedFlagGetter.get();
      }
    };

    mySearchFieldWrapper = new MyTextComponentWrapper() {
      @Override
      public void setContent(JComponent wrapped) {
        super.setContent(wrapped);
        mySearchTextComponent = unwrapTextComponent(wrapped);
      }
    };
    myReplaceFieldWrapper = new MyTextComponentWrapper() {
      @Override
      public void setContent(JComponent wrapped) {
        super.setContent(wrapped);
        myReplaceTextComponent = unwrapTextComponent(wrapped);
      }
    };

    myLeftPanel = new NonOpaquePanel(new BorderLayout());
    myLeftPanel.setBorder(JBUI.Borders.emptyLeft(6));
    myLeftPanel.add(mySearchFieldWrapper, BorderLayout.NORTH);
    myLeftPanel.add(myReplaceFieldWrapper, BorderLayout.SOUTH);

    mySearchActionsToolbar1 = createSearchToolbar1(searchToolbar1Actions);
    Wrapper searchToolbarWrapper1 = new NonOpaquePanel(new BorderLayout());
    searchToolbarWrapper1.add(mySearchActionsToolbar1, BorderLayout.WEST);
    mySearchActionsToolbar2 = createSearchToolbar2(searchToolbar2Actions);
    mySearchActionsToolbar2.setForceShowFirstComponent(true);
    Wrapper searchToolbarWrapper2 = new Wrapper(mySearchActionsToolbar2);
    mySearchActionsToolbar2.setBorder(JBUI.Borders.emptyLeft(16));
    JPanel searchPair = new NonOpaquePanel(new BorderLayout()).setVerticalSizeReferent(mySearchFieldWrapper);
    searchPair.add(searchToolbarWrapper1, BorderLayout.WEST);
    searchPair.add(searchToolbarWrapper2, BorderLayout.CENTER);

    myReplaceActionsToolbar1 = createReplaceToolbar1(replaceToolbar1Actions);
    Wrapper replaceToolbarWrapper1 = new Wrapper(myReplaceActionsToolbar1).setVerticalSizeReferent(myReplaceFieldWrapper);
    myReplaceActionsToolbar2 = createReplaceToolbar2(replaceToolbar2Actions);
    myReplaceActionsToolbar2.setForceShowFirstComponent(true);
    Wrapper replaceToolbarWrapper2 = new Wrapper(myReplaceActionsToolbar2).setVerticalSizeReferent(myReplaceFieldWrapper);
    myReplaceActionsToolbar2.setBorder(JBUI.Borders.emptyLeft(16));
    myReplaceToolbarWrapper = new NonOpaquePanel(new BorderLayout());
    myReplaceToolbarWrapper.add(replaceToolbarWrapper1, BorderLayout.WEST);
    myReplaceToolbarWrapper.add(replaceToolbarWrapper2, BorderLayout.CENTER);

    searchToolbarWrapper1.setHorizontalSizeReferent(replaceToolbarWrapper1);

    JLabel closeLabel = new JLabel(null, AllIcons.Actions.Close, SwingConstants.RIGHT);
    closeLabel.setBorder(JBUI.Borders.empty(2 ));
    closeLabel.setVerticalAlignment(SwingConstants.TOP);
    closeLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        close();
      }
    });
    closeLabel.setToolTipText("Close search bar (Escape)");
    searchPair.add(new Wrapper.North(closeLabel), BorderLayout.EAST);

    myRightPanel = new NonOpaquePanel(new BorderLayout()) {
      @Override
      public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.width += JBUIScale.scale(16);//looks like hack but we need this extra space in case of lack of width
        return size;
      }
    };
    myRightPanel.add(searchPair, BorderLayout.NORTH);
    myRightPanel.add(myReplaceToolbarWrapper, BorderLayout.CENTER);

    OnePixelSplitter splitter = new OnePixelSplitter(false, .25F);
    myRightPanel.setBorder(JBUI.Borders.emptyLeft(6));
    splitter.setFirstComponent(myLeftPanel);
    splitter.setSecondComponent(myRightPanel);
    splitter.setHonorComponentsMinimumSize(true);
    splitter.setLackOfSpaceStrategy(Splitter.LackOfSpaceStrategy.HONOR_THE_SECOND_MIN_SIZE);
    splitter.setAndLoadSplitterProportionKey("FindSplitterProportion");
    splitter.setOpaque(false);
    splitter.getDivider().setOpaque(false);
    add(splitter, BorderLayout.CENTER);

    update("", "", false, false);

    // it's assigned after all action updates so that actions don't get access to uninitialized components
    myDataProviderDelegate = dataProvider;

    setFocusCycleRoot(true);

    setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
  }

  public void resetUndoRedoActions() {
    UIUtil.resetUndoRedoActions(mySearchTextComponent);
    UIUtil.resetUndoRedoActions(myReplaceTextComponent);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();

    addTextToRecent(mySearchTextComponent);
    if (myReplaceTextComponent != null) {
      addTextToRecent(myReplaceTextComponent);
    }
  }

  public void requestFocusInTheSearchFieldAndSelectContent(Project project) {
    mySearchTextComponent.selectAll();
    IdeFocusManager.getInstance(project).requestFocus(mySearchTextComponent, true);
    if (myReplaceTextComponent != null) {
      myReplaceTextComponent.selectAll();
    }
  }

  public void setStatusText(@NotNull String status) {
    myStatusText = status;
  }

  @NotNull
  public String getStatusText() {
    return myStatusText;
  }

  public void replace() {
    if (myReplaceAction != null) {
      myReplaceAction.run();
    }
  }

  public void close() {
    if (myCloseAction != null) {
      myCloseAction.run();
    }
  }

  public void setRegularBackground() {
    mySearchTextComponent.setBackground(UIUtil.getTextFieldBackground());
  }

  public void setNotFoundBackground() {
    mySearchTextComponent.setBackground(LightColors.RED);
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (SpeedSearchSupply.SPEED_SEARCH_CURRENT_QUERY.is(dataId)) {
      return mySearchTextComponent.getText();
    }
    if (TouchbarDataKeys.ACTIONS_KEY.is(dataId)) {
      if (myTouchbarActions == null) {
        myTouchbarActions = new DefaultActionGroup();
        myTouchbarActions.add(new PrevOccurrenceAction());
        myTouchbarActions.add(new NextOccurrenceAction());
      }
      return myTouchbarActions;
    }
    return myDataProviderDelegate != null ? myDataProviderDelegate.getData(dataId) : null;
  }

  public Project getProject() {
    return myProject;
  }

  public void addListener(@NotNull Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  public boolean isMultiline() {
    return myMultilineMode;
  }

  private void setMultilineInternal(boolean multiline) {
    boolean stateChanged = multiline != myMultilineMode;
    myMultilineMode = multiline;
    if (stateChanged) {
      multilineStateChanged();
    }
  }

  @NotNull
  public JTextComponent getSearchTextComponent() {
    return mySearchTextComponent;
  }

  @NotNull
  public JTextComponent getReplaceTextComponent() {
    return myReplaceTextComponent;
  }


  private void updateSearchComponent(@NotNull String textToSet) {
    if (!updateTextComponent(true)) {
      replaceTextInTextComponentEnsuringSelection(textToSet, mySearchTextComponent);
      return;
    }

    mySearchTextComponent.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        ApplicationManager.getApplication().invokeLater(() -> searchFieldDocumentChanged());
      }
    });

    mySearchTextComponent.registerKeyboardAction(new ActionListener() {
                                                   @Override
                                                   public void actionPerformed(final ActionEvent e) {
                                                     if (StringUtil.isEmpty(mySearchTextComponent.getText())) {
                                                       close();
                                                     }
                                                     else {
                                                       IdeFocusManager.getInstance(myProject).requestFocus(myTargetComponent, true);
                                                       addTextToRecent(mySearchTextComponent);
                                                     }
                                                   }
                                                 }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, SystemInfo.isMac
                                                                                              ? META_DOWN_MASK : CTRL_DOWN_MASK),
                                                 JComponent.WHEN_FOCUSED);

    new VariantsCompletionAction(mySearchTextComponent); // It registers a shortcut set automatically on construction
  }

  private static void replaceTextInTextComponentEnsuringSelection(@NotNull String textToSet, JTextComponent component) {
    String existingText = component.getText();
    if (!existingText.equals(textToSet)) {
      component.setText(textToSet);
      // textToSet should be selected even if we have no selection before (if we have the selection then setText will remain it)
      if (component.getSelectionStart() == component.getSelectionEnd()) component.selectAll();
    }
  }

  private void updateReplaceComponent(@NotNull String textToSet) {
    if (!updateTextComponent(false)) {
      replaceTextInTextComponentEnsuringSelection(textToSet, myReplaceTextComponent);
      return;
    }
    myReplaceTextComponent.setText(textToSet);

    myReplaceTextComponent.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        ApplicationManager.getApplication().invokeLater(() -> replaceFieldDocumentChanged());
      }
    });

    if (!isMultiline()) {
      installReplaceOnEnterAction(myReplaceTextComponent);
    }

    new VariantsCompletionAction(myReplaceTextComponent);
    myReplaceFieldWrapper.revalidate();
    myReplaceFieldWrapper.repaint();
  }

  public void update(@NotNull String findText, @NotNull String replaceText, boolean replaceMode, boolean multiline) {
    setMultilineInternal(multiline);
    boolean needToResetSearchFocus = mySearchTextComponent != null && mySearchTextComponent.hasFocus();
    boolean needToResetReplaceFocus = myReplaceTextComponent != null && myReplaceTextComponent.hasFocus();
    updateSearchComponent(findText);
    updateReplaceComponent(replaceText);
    if (replaceMode) {
      if (myReplaceFieldWrapper.getParent() == null) {
        myLeftPanel.add(myReplaceFieldWrapper, BorderLayout.CENTER);
      }
      if (myReplaceToolbarWrapper.getParent() == null) {
        myRightPanel.add(myReplaceToolbarWrapper, BorderLayout.CENTER);
      }
      if (needToResetReplaceFocus) {
        myReplaceTextComponent.requestFocusInWindow();
      }
    }
    else {
      if (myReplaceFieldWrapper.getParent() != null) {
        myLeftPanel.remove(myReplaceFieldWrapper);
      }
      if (myReplaceToolbarWrapper.getParent() != null) {
        myRightPanel.remove(myReplaceToolbarWrapper);
      }
    }
    if (needToResetSearchFocus) mySearchTextComponent.requestFocusInWindow();
    updateBindings();
    updateActions();
    revalidate();
    repaint();
  }

  public void updateActions() {
    mySearchActionsToolbar1.updateActionsImmediately();
    mySearchActionsToolbar2.updateActionsImmediately();
    myReplaceActionsToolbar1.updateActionsImmediately();
    myReplaceActionsToolbar2.updateActionsImmediately();
  }

  public void addTextToRecent(@NotNull JTextComponent textField) {
    final String text = textField.getText();
    if (text.length() > 0) {
      FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(myProject);
      if (textField == mySearchTextComponent) {
        findInProjectSettings.addStringToFind(text);
        if (mySearchFieldWrapper.getTargetComponent() instanceof SearchTextField) {
          ((SearchTextField)mySearchFieldWrapper.getTargetComponent()).addCurrentTextToHistory();
        }
      }
      else {
        findInProjectSettings.addStringToReplace(text);
        if (myReplaceFieldWrapper.getTargetComponent() instanceof SearchTextField) {
          ((SearchTextField)myReplaceFieldWrapper.getTargetComponent()).addCurrentTextToHistory();
        }
      }
    }
  }

  private boolean updateTextComponent(boolean search) {
    JTextComponent oldComponent = search ? mySearchTextComponent : myReplaceTextComponent;
    if (oldComponent != null) return false;
    final MyTextComponentWrapper wrapper = search ? mySearchFieldWrapper : myReplaceFieldWrapper;

    final JTextArea textComponent;
      SearchTextArea textArea = new SearchTextArea(search);
      textComponent = textArea.getTextArea();
      textComponent.setRows(isMultiline() ? 2 : 1);

    wrapper.setContent(textArea);

    UIUtil.addUndoRedoActions(textComponent);

    textComponent.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, Boolean.TRUE);
    textComponent.setBackground(UIUtil.getTextFieldBackground());
    textComponent.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {
        textComponent.repaint();
      }

      @Override
      public void focusLost(final FocusEvent e) {
        textComponent.repaint();
      }
    });
    installCloseOnEscapeAction(textComponent);
    return true;
  }

  private void searchFieldDocumentChanged() {
    if (mySearchTextComponent instanceof JTextArea) {
      adjustRows((JTextArea)mySearchTextComponent);
    }
    myEventDispatcher.getMulticaster().searchFieldDocumentChanged();
  }

  private void replaceFieldDocumentChanged() {
    if (myReplaceTextComponent instanceof JTextArea) {
      adjustRows((JTextArea)myReplaceTextComponent);
    }
    myReplaceActionsToolbar2.invalidate();
    doLayout();
    myEventDispatcher.getMulticaster().replaceFieldDocumentChanged();
  }

  private void multilineStateChanged() {
    myEventDispatcher.getMulticaster().multilineStateChanged();
  }

  private static void adjustRows(@NotNull JTextArea area) {
    area.setRows(Math.max(1, Math.min(3, StringUtil.countChars(area.getText(), '\n') + 1)));
  }

  private void installCloseOnEscapeAction(@NotNull JTextComponent c) {
    new DumbAwareAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        close();
      }
    }.registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_EDITOR_ESCAPE), c);
  }

  private void installReplaceOnEnterAction(@NotNull JTextComponent c) {
    ActionListener action = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        replace();
      }
    };
    c.registerKeyboardAction(action, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
  }

  private void updateBindings() {
    updateBindings(mySearchFieldActions, mySearchFieldWrapper);
    updateBindings(mySearchActionsToolbar1, mySearchFieldWrapper);
    updateBindings(mySearchActionsToolbar2, mySearchFieldWrapper);

    updateBindings(myReplaceFieldActions, myReplaceFieldWrapper);
    updateBindings(myReplaceActionsToolbar1, myReplaceToolbarWrapper);
    updateBindings(myReplaceActionsToolbar2, myReplaceToolbarWrapper);
  }

  private void updateBindings(@NotNull DefaultActionGroup group, @NotNull JComponent shortcutHolder) {
    updateBindings(ContainerUtil.immutableList(group.getChildActionsOrStubs()), shortcutHolder);
  }

  private void updateBindings(@NotNull ActionToolbarImpl toolbar, @NotNull JComponent shortcutHolder) {
    updateBindings(toolbar.getActions(), shortcutHolder);
  }

  private void updateBindings(@NotNull List<? extends AnAction> actions, @NotNull JComponent shortcutHolder) {
    DataContext context = DataManager.getInstance().getDataContext(this);
    for (AnAction action : actions) {
      ShortcutSet shortcut = null;
      if (action instanceof ContextAwareShortcutProvider) {
        shortcut = ((ContextAwareShortcutProvider)action).getShortcut(context);
      }
      else if (action instanceof ShortcutProvider) {
        shortcut = ((ShortcutProvider)action).getShortcut();
      }
      if (shortcut != null) {
        action.registerCustomShortcutSet(shortcut, shortcutHolder);
      }
    }
  }


  @NotNull
  private ActionToolbarImpl createSearchToolbar1(@NotNull DefaultActionGroup group) {
    ActionToolbarImpl toolbar = createToolbar(group);
    toolbar.setForceMinimumSize(true);
    toolbar.setReservePlaceAutoPopupIcon(false);
    toolbar.setSecondaryButtonPopupStateModifier(mySearchToolbar1PopupStateModifier);
    KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
    if (keyboardShortcut != null) {
      toolbar.setSecondaryActionsTooltip(FindBundle.message("find.popup.show.filter.popup") + " (" + KeymapUtil.getShortcutText(keyboardShortcut) + ")");
    } else {
      toolbar.setSecondaryActionsTooltip(FindBundle.message("find.popup.show.filter.popup"));
    }
    toolbar.setSecondaryActionsIcon(AllIcons.General.Filter);

    new ShowMoreOptions(toolbar, mySearchFieldWrapper);
    return toolbar;
  }

  @NotNull
  private ActionToolbarImpl createSearchToolbar2(@NotNull DefaultActionGroup group) {
    return createToolbar(group);
  }

  @NotNull
  private ActionToolbarImpl createReplaceToolbar1(@NotNull DefaultActionGroup group) {
    ActionToolbarImpl toolbar = createToolbar(group);
    toolbar.setForceMinimumSize(true);
    toolbar.setReservePlaceAutoPopupIcon(false);
    return toolbar;
  }

  @NotNull
  private ActionToolbarImpl createReplaceToolbar2(@NotNull DefaultActionGroup group) {
    return createToolbar(group);
  }

  @NotNull
  private ActionToolbarImpl createToolbar(@NotNull ActionGroup group) {
    ActionToolbarImpl toolbar = (ActionToolbarImpl)ActionManager.getInstance()
      .createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, group, true);
    toolbar.setBorder(JBUI.Borders.empty());
    toolbar.setTargetComponent(this);
    toolbar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
    Utils.setSmallerFontForChildren(toolbar);
    return toolbar;
  }

  public interface Listener extends EventListener {
    void searchFieldDocumentChanged();

    void replaceFieldDocumentChanged();

    void multilineStateChanged();
  }

  public static class Builder {
    private final Project myProject;
    private final JComponent myTargetComponent;

    private DataProvider myDataProvider;

    private Runnable myReplaceAction;
    private Runnable myCloseAction;

    private final DefaultActionGroup mySearchActions = new DefaultActionGroup("search bar 1", false);
    private final DefaultActionGroup myExtraSearchActions = new DefaultActionGroup("search bar 2", false);
    private final DefaultActionGroup mySearchFieldActions = new DefaultActionGroup("search field actions", false);
    private BooleanGetter mySearchToolbarModifiedFlagGetter = BooleanGetter.FALSE;

    private final DefaultActionGroup myReplaceActions = new DefaultActionGroup("replace bar 1", false);
    private final DefaultActionGroup myExtraReplaceActions = new DefaultActionGroup("replace bar 1", false);
    private final DefaultActionGroup myReplaceFieldActions = new DefaultActionGroup("replace field actions", false);

    private Builder(@Nullable Project project, @NotNull JComponent component) {
      myProject = project;
      myTargetComponent = component;
    }

    @NotNull
    public Builder withDataProvider(@NotNull DataProvider provider) {
      myDataProvider = provider;
      return this;
    }

    @NotNull
    public Builder withReplaceAction(@NotNull Runnable action) {
      myReplaceAction = action;
      return this;
    }

    @NotNull
    public Builder withCloseAction(@NotNull Runnable action) {
      myCloseAction = action;
      return this;
    }

    @NotNull
    public Builder addSearchFieldActions(@NotNull AnAction... actions) {
      mySearchFieldActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addReplaceFieldActions(@NotNull AnAction... actions) {
      myReplaceFieldActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addPrimarySearchActions(@NotNull AnAction... actions) {
      mySearchActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addSecondarySearchActions(@NotNull AnAction... actions) {
      for (AnAction action : actions) {
        mySearchActions.addAction(action).setAsSecondary(true);
      }
      return this;
    }

    @NotNull
    public Builder withSecondarySearchActionsIsModifiedGetter(@NotNull BooleanGetter getter) {
      mySearchToolbarModifiedFlagGetter = getter;
      return this;
    }

    @NotNull
    public Builder addExtraSearchActions(@NotNull AnAction... actions) {
      myExtraSearchActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addPrimaryReplaceActions(@NotNull AnAction... actions) {
      myReplaceActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addExtraReplaceAction(@NotNull AnAction... actions) {
      myExtraReplaceActions.addAll(actions);
      return this;
    }

    @NotNull
    public SearchReplaceComponent build() {
      return new SearchReplaceComponent(myProject,
                                        myTargetComponent,
                                        mySearchActions,
                                        mySearchToolbarModifiedFlagGetter,
                                        myExtraSearchActions,
                                        mySearchFieldActions,
                                        myReplaceActions,
                                        myExtraReplaceActions,
                                        myReplaceFieldActions,
                                        myReplaceAction,
                                        myCloseAction,
                                        myDataProvider);
    }
  }

  private static class MyTextComponentWrapper extends Wrapper {
    @Nullable
    public JTextComponent getTextComponent() {
      JComponent wrapped = getTargetComponent();
      return wrapped != null ? unwrapTextComponent(wrapped) : null;
    }

    @NotNull
    protected static JTextComponent unwrapTextComponent(@NotNull JComponent wrapped) {
      if (wrapped instanceof SearchTextField) {
        return ((SearchTextField)wrapped).getTextEditor();
      }
      if (wrapped instanceof SearchTextArea) {
        return ((SearchTextArea)wrapped).getTextArea();
      }
      throw new AssertionError();
    }
  }
}
