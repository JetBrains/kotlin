// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.find.editorHeaderActions.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.lightEdit.LightEditCompatible;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.EditorHeaderComponent;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.BooleanGetter;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.*;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.ui.mac.TouchbarDataKeys;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.util.BooleanFunction;
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
import java.util.ArrayList;
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
  private final ActionToolbarImpl mySearchActionsToolbar;
  private final List<AnAction> myEmbeddedSearchActions = new ArrayList<>();
  private final List<Component> myExtraSearchButtons = new ArrayList<>();
  private final BooleanGetter mySearchToolbarModifiedFlagGetter;

  private final DefaultActionGroup myReplaceFieldActions;
  private final ActionToolbarImpl myReplaceActionsToolbar;
  private final List<AnAction> myEmbeddedReplaceActions = new ArrayList<>();
  private final List<Component> myExtraReplaceButtons = new ArrayList<>();

  private final JPanel myReplaceToolbarWrapper;

  private final Project myProject;
  private final JComponent myTargetComponent;

  private final Runnable myCloseAction;
  private final Runnable myReplaceAction;

  private final DataProvider myDataProviderDelegate;

  private boolean myMultilineMode;
  @NotNull private String myStatusText = "";
  @NotNull private Color myStatusColor = UIUtil.getLabelForeground();
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
    mySearchToolbarModifiedFlagGetter = searchToolbar1ModifiedFlagGetter;
    mySearchFieldActions = searchFieldActions;
    myReplaceFieldActions = replaceFieldActions;
    myReplaceAction = replaceAction;
    myCloseAction = closeAction;

    for (AnAction child : searchToolbar2Actions.getChildren(null)) {
      if (child instanceof Embeddable) {
        myEmbeddedSearchActions.add(child);
        ShortcutSet shortcutSet = ActionUtil.getMnemonicAsShortcut(child);
        if (shortcutSet != null) child.registerCustomShortcutSet(shortcutSet, this);
      }
    }
    for (AnAction action : myEmbeddedSearchActions) {
      searchToolbar2Actions.remove(action);
    }
    for (AnAction child : replaceToolbar2Actions.getChildren(null)) {
      if (child instanceof Embeddable) {
        myEmbeddedReplaceActions.add(child);
        ShortcutSet shortcutSet = ActionUtil.getMnemonicAsShortcut(child);
        if (shortcutSet != null) child.registerCustomShortcutSet(shortcutSet, this);
      }
    }
    for (AnAction action : myEmbeddedReplaceActions) {
      replaceToolbar2Actions.remove(action);
    }

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
    myReplaceFieldWrapper.setBorder(JBUI.Borders.emptyTop(1));

    myLeftPanel = new JPanel(new GridBagLayout());
    myLeftPanel.setBackground(JBColor.border());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1;
    constraints.weighty = 1;
    myLeftPanel.add(mySearchFieldWrapper, constraints);
    constraints.gridy++;
    myLeftPanel.add(myReplaceFieldWrapper, constraints);
    myLeftPanel.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1));

    searchToolbar1Actions.addAll(searchToolbar2Actions.getChildren(null));
    replaceToolbar1Actions.addAll(replaceToolbar2Actions.getChildren(null));

    mySearchActionsToolbar = createSearchToolbar1(searchToolbar1Actions);
    mySearchActionsToolbar.setForceShowFirstComponent(true);
    JPanel searchPair = new NonOpaquePanel(new BorderLayout());
    searchPair.add(mySearchActionsToolbar, BorderLayout.CENTER);

    myReplaceActionsToolbar = createReplaceToolbar1(replaceToolbar1Actions);
    myReplaceActionsToolbar.setBorder(JBUI.Borders.empty());
    Wrapper replaceToolbarWrapper1 = new Wrapper(myReplaceActionsToolbar);
    myReplaceToolbarWrapper = new NonOpaquePanel(new BorderLayout());
    myReplaceToolbarWrapper.add(replaceToolbarWrapper1, BorderLayout.WEST);
    myReplaceToolbarWrapper.setBorder(JBUI.Borders.emptyTop(3));

    JLabel closeLabel = new JLabel(null, AllIcons.Actions.Close, SwingConstants.RIGHT);
    closeLabel.setBorder(JBUI.Borders.empty(2));
    closeLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        close();
      }
    });
    closeLabel.setToolTipText(FindBundle.message("tooltip.close.search.bar.escape"));
    searchPair.add(new Wrapper(closeLabel), BorderLayout.EAST);

    myRightPanel = new NonOpaquePanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
    myRightPanel.add(searchPair);
    myRightPanel.add(myReplaceToolbarWrapper);

    OnePixelSplitter splitter = new OnePixelSplitter(false, .33F);
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
    // A workaround to suppress editor-specific TabAction
    new DumbAwareAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        Component focusOwner = IdeFocusManager.getInstance(myProject).getFocusOwner();
        if (UIUtil.isAncestor(SearchReplaceComponent.this, focusOwner)) focusOwner.transferFocus();
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), this);
    new DumbAwareAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        Component focusOwner = IdeFocusManager.getInstance(myProject).getFocusOwner();
        if (UIUtil.isAncestor(SearchReplaceComponent.this, focusOwner)) focusOwner.transferFocusBackward();
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)), this);
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

  @NotNull
  public Color getStatusColor() {
    return myStatusColor;
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
    myStatusColor = UIUtil.getLabelForeground();
  }

  public void setNotFoundBackground() {
    mySearchTextComponent.setBackground(LightColors.RED);
    myStatusColor = UIUtil.getErrorForeground();
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
      myEventDispatcher.getMulticaster().multilineStateChanged();
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
        ApplicationManager.getApplication().invokeLater(() -> myEventDispatcher.getMulticaster().searchFieldDocumentChanged());
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
    // make sure Enter is consumed by search text field, even if 'next occurrence' action is disabled
    // this is needed to e.g. avoid triggering a default button in containing dialog (see IDEA-128057)
    mySearchTextComponent.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {}
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), WHEN_FOCUSED);

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
        ApplicationManager.getApplication().invokeLater(() -> myEventDispatcher.getMulticaster().replaceFieldDocumentChanged());
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
    myReplaceFieldWrapper.setVisible(replaceMode);
    myReplaceToolbarWrapper.setVisible(replaceMode);
    if (needToResetReplaceFocus) myReplaceTextComponent.requestFocusInWindow();
    if (needToResetSearchFocus) mySearchTextComponent.requestFocusInWindow();
    updateBindings();
    updateActions();
    List<Component> focusOrder = new ArrayList<>();
    focusOrder.add(mySearchTextComponent);
    focusOrder.add(myReplaceTextComponent);
    focusOrder.addAll(myExtraSearchButtons);
    focusOrder.addAll(myExtraReplaceButtons);
    setFocusCycleRoot(true);
    setFocusTraversalPolicy(new ListFocusTraversalPolicy(focusOrder));
    revalidate();
    repaint();
  }

  public void updateActions() {
    mySearchActionsToolbar.updateActionsImmediately();
    myReplaceActionsToolbar.updateActionsImmediately();
    JComponent textComponent = mySearchFieldWrapper.getTargetComponent();
    if (textComponent instanceof SearchTextArea) ((SearchTextArea)textComponent).updateExtraActions();
    textComponent = myReplaceFieldWrapper.getTargetComponent();
    if (textComponent instanceof SearchTextArea) ((SearchTextArea)textComponent).updateExtraActions();
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

    final JBTextArea textComponent = new JBTextArea();
    textComponent.setRows(isMultiline() ? 2 : 1);
    textComponent.setColumns(12);
    if (search) {
      textComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.search.accessible.name"));
    }
    else {
      textComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.replace.accessible.name"));
    }
    SearchTextArea textArea = new SearchTextArea(textComponent, search);
    if (search) {
      myExtraSearchButtons.clear();
      myExtraSearchButtons.addAll(textArea.setExtraActions(myEmbeddedSearchActions.toArray(AnAction.EMPTY_ARRAY)));
    } else {
      myExtraReplaceButtons.clear();
      myExtraReplaceButtons.addAll(textArea.setExtraActions(myEmbeddedReplaceActions.toArray(AnAction.EMPTY_ARRAY)));
    }
    // Display empty text only when focused
    textComponent.putClientProperty(
      "StatusVisibleFunction", (BooleanFunction<JTextComponent>)(c -> c.getText().isEmpty() && c.isFocusOwner()));

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
    new CloseAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        close();
      }
    }.registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_EDITOR_ESCAPE), textArea);
    return true;
  }

  private abstract static class CloseAction extends DumbAwareAction implements LightEditCompatible {
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
    updateBindings(mySearchActionsToolbar, mySearchFieldWrapper);

    updateBindings(myReplaceFieldActions, myReplaceFieldWrapper);
    updateBindings(myReplaceActionsToolbar, myReplaceToolbarWrapper);
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
    toolbar.setSecondaryActionsTooltip(FindBundle.message("find.popup.show.filter.popup"));
    toolbar.setSecondaryActionsIcon(AllIcons.General.Filter, true);
    toolbar.setNoGapMode();
    toolbar.setSecondaryButtonPopupStateModifier(new ActionToolbarImpl.SecondaryGroupUpdater() {
      @Override
      public void update(@NotNull AnActionEvent e) {
        Icon icon = e.getPresentation().getIcon();
        if (icon != null && mySearchToolbarModifiedFlagGetter.get()) {
          e.getPresentation().setIcon(ExecutionUtil.getLiveIndicator(icon));
        }
      }
    });

    KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
    if (keyboardShortcut != null) {
      toolbar.setSecondaryActionsShortcut(KeymapUtil.getShortcutText(keyboardShortcut));
    }

    new ShowMoreOptions(toolbar, mySearchFieldWrapper);
    return toolbar;
  }

  @NotNull
  private ActionToolbarImpl createReplaceToolbar1(@NotNull DefaultActionGroup group) {
    ActionToolbarImpl toolbar = createToolbar(group);
    toolbar.setForceMinimumSize(true);
    toolbar.setReservePlaceAutoPopupIcon(false);
    return toolbar;
  }

  @NotNull
  private ActionToolbarImpl createToolbar(@NotNull ActionGroup group) {
    ActionToolbarImpl toolbar = (ActionToolbarImpl)ActionManager.getInstance()
      .createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, group, true);
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

  @SuppressWarnings("HardCodedStringLiteral")
  public static class Builder {
    private final Project myProject;
    private final JComponent myTargetComponent;

    private DataProvider myDataProvider;

    private Runnable myReplaceAction;
    private Runnable myCloseAction;

    private final DefaultActionGroup mySearchActions = DefaultActionGroup.createFlatGroup(() -> "search bar 1");
    private final DefaultActionGroup myExtraSearchActions = DefaultActionGroup.createFlatGroup(() -> "search bar 2");
    private final DefaultActionGroup mySearchFieldActions = DefaultActionGroup.createFlatGroup(() -> "search field actions");
    private BooleanGetter mySearchToolbarModifiedFlagGetter = BooleanGetter.FALSE;

    private final DefaultActionGroup myReplaceActions = DefaultActionGroup.createFlatGroup(() -> "replace bar 1");
    private final DefaultActionGroup myExtraReplaceActions = DefaultActionGroup.createFlatGroup(() -> "replace bar 1");
    private final DefaultActionGroup myReplaceFieldActions = DefaultActionGroup.createFlatGroup(() -> "replace field actions");

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
    public Builder addSearchFieldActions(AnAction @NotNull ... actions) {
      mySearchFieldActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addReplaceFieldActions(AnAction @NotNull ... actions) {
      myReplaceFieldActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addPrimarySearchActions(AnAction @NotNull ... actions) {
      mySearchActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addSecondarySearchActions(AnAction @NotNull ... actions) {
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
    public Builder addExtraSearchActions(AnAction @NotNull ... actions) {
      myExtraSearchActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addPrimaryReplaceActions(AnAction @NotNull ... actions) {
      myReplaceActions.addAll(actions);
      return this;
    }

    @NotNull
    public Builder addExtraReplaceAction(AnAction @NotNull ... actions) {
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
