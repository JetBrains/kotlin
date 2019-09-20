// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.QualifiedNameProviderUtil;
import com.intellij.ide.actions.SearchEverywherePsiRenderer;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.ide.util.gotoByName.*;
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo;
import com.intellij.ide.util.scopeChooser.ScopeDescriptor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.OffsetIcon;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractGotoSEContributor implements WeightedSearchEverywhereContributor<Object> {
  private static final Logger LOG = Logger.getInstance(AbstractGotoSEContributor.class);
  private static final Key<Map<String, String>> SE_SELECTED_SCOPES = Key.create("SE_SELECTED_SCOPES");

  private static final Pattern ourPatternToDetectLinesAndColumns = Pattern.compile(
    "(.+?)" + // name, non-greedy matching
    "(?::|@|,| |#|#L|\\?l=| on line | at line |:?\\(|:?\\[)" + // separator
    "(\\d+)?(?:\\W(\\d+)?)?" + // line + column
    "[)\\]]?" // possible closing paren/brace
  );

  protected final Project myProject;
  protected final PsiElement psiContext;
  protected boolean myEverywhere;
  protected ScopeDescriptor myScopeDescriptor;

  private final GlobalSearchScope myEverywhereScope;
  private final GlobalSearchScope myProjectScope;

  protected AbstractGotoSEContributor(@Nullable Project project, @Nullable PsiElement context) {
    myProject = project;
    psiContext = context;
    myEverywhereScope = myProject == null ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.everythingScope(myProject);
    GlobalSearchScope projectScope = myProject == null ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.projectScope(myProject);
    if (myProject == null) {
      myProjectScope = GlobalSearchScope.EMPTY_SCOPE;
    }
    else if (!myEverywhereScope.equals(projectScope)) {
      myProjectScope = projectScope;
    }
    else {
      // just get the second scope, i.e. Attached Directories in DataGrip
      Ref<GlobalSearchScope> result = Ref.create();
      processScopes(SimpleDataContext.getProjectContext(myProject), o -> {
        if (o.scopeEquals(myEverywhereScope) || o.scopeEquals(null)) return true;
        result.set((GlobalSearchScope)o.getScope());
        return false;
      });
      myProjectScope = ObjectUtils.notNull(result.get(), myEverywhereScope);
    }
    myScopeDescriptor = getInitialSelectedScope();
  }

  @NotNull
  @Override
  public String getSearchProviderId() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isShownInSeparateTab() {
    return true;
  }

  private static void processScopes(@NotNull DataContext dataContext, @NotNull Processor<? super ScopeDescriptor> processor) {
    Project project = ObjectUtils.notNull(CommonDataKeys.PROJECT.getData(dataContext));
    ScopeChooserCombo.processScopes(project, dataContext, ScopeChooserCombo.OPT_LIBRARIES | ScopeChooserCombo.OPT_EMPTY_SCOPES, processor);
  }

  @NotNull
  protected List<AnAction> doGetActions(@NotNull String everywhereText,
                                        @Nullable PersistentSearchEverywhereContributorFilter<?> filter,
                                        @NotNull Runnable onChanged) {
    if (myProject == null || filter == null) return Collections.emptyList();
    ArrayList<AnAction> result = new ArrayList<>();
    if (Registry.is("search.everywhere.show.scopes")) {
      result.add(new ScopeChooserAction() {
        final boolean canToggleEverywhere = !myEverywhereScope.equals(myProjectScope);

        @Override
        void onScopeSelected(@NotNull ScopeDescriptor o) {
          setSelectedScope(o);
          onChanged.run();
        }

        @NotNull
        @Override
        ScopeDescriptor getSelectedScope() {
          return myScopeDescriptor;
        }

        @Override
        void onProjectScopeToggled() {
          setEverywhere(!myScopeDescriptor.scopeEquals(myEverywhereScope));
        }

        @Override
        public boolean isEverywhere() {
          return myScopeDescriptor.scopeEquals(myEverywhereScope);
        }

        @Override
        public void setEverywhere(boolean everywhere) {
          setSelectedScope(new ScopeDescriptor(everywhere ? myEverywhereScope : myProjectScope));
          onChanged.run();
        }

        @Override
        public boolean canToggleEverywhere() {
          if (!canToggleEverywhere) return false;
          return myScopeDescriptor.scopeEquals(myEverywhereScope) ||
                 myScopeDescriptor.scopeEquals(myProjectScope);
        }
      });
    }
    else {
      result.add(new CheckBoxSearchEverywhereToggleAction(everywhereText) {
        @Override
        public boolean isEverywhere() {
          return myEverywhere;
        }

        @Override
        public void setEverywhere(boolean state) {
          myEverywhere = state;
          onChanged.run();
        }
      });
    }
    result.add(new SearchEverywhereUI.FiltersAction(filter, onChanged));
    return result;
  }

  @NotNull
  private ScopeDescriptor getInitialSelectedScope() {
    String selectedScope = myProject == null ? null : getSelectedScopes(myProject).get(getClass().getSimpleName());
    if (Registry.is("search.everywhere.show.scopes") && Registry.is("search.everywhere.sticky.scopes") &&
        StringUtil.isNotEmpty(selectedScope)) {
      Ref<ScopeDescriptor> result = Ref.create();
      processScopes(SimpleDataContext.getProjectContext(myProject), o -> {
        if (!selectedScope.equals(o.getDisplayName()) || o.scopeEquals(null)) return true;
        result.set(o);
        return false;
      });
      return !result.isNull() ? result.get() : new ScopeDescriptor(myProjectScope);
    }
    else {
      return new ScopeDescriptor(myProjectScope);
    }
  }

  private void setSelectedScope(@NotNull ScopeDescriptor o) {
    myScopeDescriptor = o;
    getSelectedScopes(myProject).put(
      getClass().getSimpleName(),
      o.scopeEquals(myEverywhereScope) || o.scopeEquals(myProjectScope) ? null : o.getDisplayName());
  }

  @NotNull
  private static Map<String, String> getSelectedScopes(@NotNull Project project) {
    Map<String, String> map = SE_SELECTED_SCOPES.get(project);
    if (map == null) SE_SELECTED_SCOPES.set(project, map = new HashMap<>(3));
    return map;
  }

  @Override
  public void fetchWeightedElements(@NotNull String pattern,
                                    @NotNull ProgressIndicator progressIndicator,
                                    @NotNull Processor<? super FoundItemDescriptor<Object>> consumer) {
    if (myProject == null) return; //nowhere to search
    if (!isEmptyPatternSupported() && pattern.isEmpty()) return;

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      ProgressIndicatorUtils.yieldToPendingWriteActions();
    }
    ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
      if (!isDumbAware() && DumbService.isDumb(myProject)) return;

      FilteringGotoByModel<?> model = createModel(myProject);
      if (progressIndicator.isCanceled()) return;

      PsiElement context = psiContext != null && psiContext.isValid() ? psiContext : null;
      ChooseByNamePopup popup = ChooseByNamePopup.createPopup(myProject, model, context);
      try {
        ChooseByNameItemProvider provider = popup.getProvider();
        GlobalSearchScope scope = Registry.is("search.everywhere.show.scopes")
                                  ? (GlobalSearchScope)ObjectUtils.notNull(myScopeDescriptor.getScope())
                                  : null;

        boolean everywhere = scope == null ? myEverywhere : scope.isSearchInLibraries();
        if (scope != null && provider instanceof ChooseByNameInScopeItemProvider) {
          FindSymbolParameters parameters = FindSymbolParameters.wrap(pattern, scope);
          ((ChooseByNameInScopeItemProvider) provider).filterElementsWithWeights(popup, parameters, progressIndicator,
                        item -> processElement(progressIndicator, consumer, model, item.getItem(), item.getWeight())
          );
        }
        else if (provider instanceof ChooseByNameWeightedItemProvider) {
          ((ChooseByNameWeightedItemProvider) provider).filterElementsWithWeights(popup, pattern, everywhere, progressIndicator,
                        item -> processElement(progressIndicator, consumer, model, item.getItem(), item.getWeight())
          );
        }
        else {
          provider.filterElements(popup, pattern, everywhere, progressIndicator,
                        element -> processElement(progressIndicator, consumer, model, element, getElementPriority(element, pattern))
          );
        }
      }
      finally {
        Disposer.dispose(popup);
      }
    }, progressIndicator);
  }

  private boolean processElement(@NotNull ProgressIndicator progressIndicator,
                                 @NotNull Processor<? super FoundItemDescriptor<Object>> consumer,
                                 FilteringGotoByModel<?> model, Object element, int degree) {
    if (progressIndicator.isCanceled()) return false;

    if (element == null) {
      LOG.error("Null returned from " + model + " in " + this);
      return true;
    }

    return consumer.process(new FoundItemDescriptor<>(element, degree));
  }

  @NotNull
  protected abstract FilteringGotoByModel<?> createModel(@NotNull Project project);

  @NotNull
  @Override
  public String filterControlSymbols(@NotNull String pattern) {
    if (StringUtil.containsAnyChar(pattern, ":,;@[( #") ||
        pattern.contains(" line ") ||
        pattern.contains("?l=")) { // quick test if reg exp should be used
      return applyPatternFilter(pattern, ourPatternToDetectLinesAndColumns);
    }

    return pattern;
  }

  protected static String applyPatternFilter(String str, Pattern regex) {
    Matcher matcher = regex.matcher(str);
    if (matcher.matches()) {
      return matcher.group(1);
    }

    return str;
  }

  @Override
  public boolean showInFindResults() {
    return true;
  }

  @Override
  public boolean processSelectedItem(@NotNull Object selected, int modifiers, @NotNull String searchText) {
    if (selected instanceof PsiElement) {
      if (!((PsiElement)selected).isValid()) {
        LOG.warn("Cannot navigate to invalid PsiElement");
        return true;
      }

      PsiElement psiElement = preparePsi((PsiElement)selected, modifiers, searchText);
      Navigatable extNavigatable = createExtendedNavigatable(psiElement, searchText, modifiers);
      if (extNavigatable != null && extNavigatable.canNavigate()) {
        extNavigatable.navigate(true);
        return true;
      }

      NavigationUtil.activateFileWithPsiElement(psiElement, openInCurrentWindow(modifiers));
    }
    else {
      EditSourceUtil.navigate(((NavigationItem)selected), true, openInCurrentWindow(modifiers));
    }

    return true;
  }

  @Override
  public Object getDataForItem(@NotNull Object element, @NotNull String dataId) {
    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      if (element instanceof PsiElement) {
        return element;
      }
      if (element instanceof DataProvider) {
        return ((DataProvider)element).getData(dataId);
      }
    }

    if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION.is(dataId) && element instanceof PsiElement) {
      return QualifiedNameProviderUtil.getQualifiedName((PsiElement)element);
    }

    return null;
  }

  @Override
  public boolean isMultiSelectionSupported() {
    return true;
  }

  @Override
  public boolean isDumbAware() {
    return DumbService.isDumbAware(createModel(myProject));
  }

  @NotNull
  @Override
  public ListCellRenderer<Object> getElementsRenderer() {
    //noinspection unchecked
    return new SERenderer();
  }

  @Override
  public int getElementPriority(@NotNull Object element, @NotNull String searchPattern) {
    return 50;
  }

  @Nullable
  protected Navigatable createExtendedNavigatable(PsiElement psi, String searchText, int modifiers) {
    VirtualFile file = PsiUtilCore.getVirtualFile(psi);
    Pair<Integer, Integer> position = getLineAndColumn(searchText);
    boolean positionSpecified = position.first >= 0 || position.second >= 0;
    if (file != null && positionSpecified) {
      OpenFileDescriptor descriptor = new OpenFileDescriptor(psi.getProject(), file, position.first, position.second);
      return descriptor.setUseCurrentWindow(openInCurrentWindow(modifiers));
    }

    return null;
  }

  protected PsiElement preparePsi(PsiElement psiElement, int modifiers, String searchText) {
    return psiElement.getNavigationElement();
  }

  protected static Pair<Integer, Integer> getLineAndColumn(String text) {
    int line = getLineAndColumnRegexpGroup(text, 2);
    int column = getLineAndColumnRegexpGroup(text, 3);

    if (line == -1 && column != -1) {
      line = 0;
    }

    return new Pair<>(line, column);
  }

  private static int getLineAndColumnRegexpGroup(String text, int groupNumber) {
    final Matcher matcher = ourPatternToDetectLinesAndColumns.matcher(text);
    if (matcher.matches()) {
      try {
        if (groupNumber <= matcher.groupCount()) {
          final String group = matcher.group(groupNumber);
          if (group != null) return Integer.parseInt(group) - 1;
        }
      }
      catch (NumberFormatException ignored) {
      }
    }

    return -1;
  }

  protected static boolean openInCurrentWindow(int modifiers) {
    return (modifiers & InputEvent.SHIFT_MASK) == 0;
  }

  protected static class SERenderer extends SearchEverywherePsiRenderer {
    @Override
    public String getElementText(PsiElement element) {
      if (element instanceof NavigationItem) {
        return Optional.ofNullable(((NavigationItem)element).getPresentation())
          .map(presentation -> presentation.getPresentableText())
          .orElse(super.getElementText(element));
      }
      return super.getElementText(element);
    }
  }

  abstract static class ScopeChooserAction extends ActionGroup
    implements CustomComponentAction, DumbAware, SearchEverywhereToggleAction {

    static final char CHOOSE = 'O';
    static final char TOGGLE = 'P';
    static final String TOGGLE_ACTION_NAME = "toggleProjectScope";

    abstract void onScopeSelected(@NotNull ScopeDescriptor o);

    @NotNull
    abstract ScopeDescriptor getSelectedScope();

    abstract void onProjectScopeToggled();

    @Override public boolean canBePerformed(@NotNull DataContext context) { return true; }
    @Override public boolean isPopup() { return true; }
    @NotNull @Override public AnAction[] getChildren(@Nullable AnActionEvent e) { return EMPTY_ARRAY; }

    @NotNull @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
      JComponent component = new ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
      UIUtil.putClientProperty(component, MnemonicHelper.MNEMONIC_CHECKER, keyCode ->
        KeyEvent.getExtendedKeyCodeForChar(TOGGLE) == keyCode ||
        KeyEvent.getExtendedKeyCodeForChar(CHOOSE) == keyCode);

      MnemonicHelper.registerMnemonicAction(component, CHOOSE);
      InputMap map = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      int mask = MnemonicHelper.getFocusAcceleratorKeyMask();
      map.put(KeyStroke.getKeyStroke(TOGGLE, mask, false), TOGGLE_ACTION_NAME);
      component.getActionMap().put(TOGGLE_ACTION_NAME, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // mimic AnAction event invocation to trigger myEverywhereAutoSet=false logic
          DataContext dataContext = DataManager.getInstance().getDataContext(component);
          KeyEvent inputEvent = new KeyEvent(
            component, KeyEvent.KEY_PRESSED, e.getWhen(), MnemonicHelper.getFocusAcceleratorKeyMask(),
            KeyEvent.getExtendedKeyCodeForChar(TOGGLE), TOGGLE);
          AnActionEvent event = AnActionEvent.createFromAnAction(
            ScopeChooserAction.this, inputEvent, ActionPlaces.TOOLBAR, dataContext);
          ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
          actionManager.fireBeforeActionPerformed(ScopeChooserAction.this, dataContext, event);
          onProjectScopeToggled();
          actionManager.fireAfterActionPerformed(ScopeChooserAction.this, dataContext, event);
        }
      });
      return component;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      ScopeDescriptor selection = getSelectedScope();
      String name = StringUtil.trimMiddle(selection.getDisplayName(), 30);
      String text = StringUtil.escapeMnemonics(name).replaceFirst("(?i)([" + TOGGLE + CHOOSE + "])", "_$1");
      e.getPresentation().setText(text);
      e.getPresentation().setIcon(OffsetIcon.getOriginalIcon(selection.getIcon()));
      String shortcutText = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(
        CHOOSE, MnemonicHelper.getFocusAcceleratorKeyMask(), true));
      String shortcutText2 = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(
        TOGGLE, MnemonicHelper.getFocusAcceleratorKeyMask(), true));
      e.getPresentation().setDescription("Choose scope (" + shortcutText + ")\n" +
                                         "Toggle scope (" + shortcutText2 + ")");
      JComponent button = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
      if (button != null) {
        button.setBackground(selection.getColor());
      }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      JComponent button = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
      if (button == null || !button.isValid()) return;
      JList<ScopeDescriptor> fakeList = new JBList<>();
      ListCellRenderer<ScopeDescriptor> renderer = new ListCellRenderer<ScopeDescriptor>() {
        final ListCellRenderer<ScopeDescriptor> delegate = ScopeChooserCombo.createDefaultRenderer();
        @Override
        public Component getListCellRendererComponent(JList<? extends ScopeDescriptor> list,
                                                      ScopeDescriptor value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
          // copied from DarculaJBPopupComboPopup.customizeListRendererComponent()
          Component component = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (component instanceof JComponent &&
              !(component instanceof JSeparator || component instanceof TitledSeparator)) {
            ((JComponent)component).setBorder(JBUI.Borders.empty(2, 8));
          }
          return component;
        }
      };
      List<ScopeDescriptor> items = new ArrayList<>();
      processScopes(e.getDataContext(), o -> {
        Component c = renderer.getListCellRendererComponent(fakeList, o, -1, false, false);
        if (c instanceof JSeparator || c instanceof TitledSeparator ||
            !o.scopeEquals(null) && o.getScope() instanceof GlobalSearchScope) {
          items.add(o);
        }
        return true;
      });
      BaseListPopupStep<ScopeDescriptor> step = new BaseListPopupStep<ScopeDescriptor>("", items) {
        @Nullable
        @Override
        public PopupStep onChosen(ScopeDescriptor selectedValue, boolean finalChoice) {
          onScopeSelected(selectedValue);
          ActionToolbar toolbar = UIUtil.uiParents(button, true).filter(ActionToolbar.class).first();
          if (toolbar != null) toolbar.updateActionsImmediately();
          return FINAL_CHOICE;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
          return true;
        }

        @NotNull
        @Override
        public String getTextFor(ScopeDescriptor value) {
          return value.getScope() instanceof GlobalSearchScope ? value.getDisplayName() : "";
        }

        @Override
        public boolean isSelectable(ScopeDescriptor value) {
          return value.getScope() instanceof GlobalSearchScope;
        }
      };
      ScopeDescriptor selection = getSelectedScope();
      step.setDefaultOptionIndex(ContainerUtil.indexOf(items, o ->
        Comparing.equal(o.getDisplayName(), selection.getDisplayName())));
      ListPopupImpl popup = new ListPopupImpl(e.getProject(), step);
      popup.setMaxRowCount(10);
      //noinspection unchecked
      popup.getList().setCellRenderer(renderer);
      popup.showUnderneathOf(button);
    }
  }
}
