// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.actions.QualifiedNameProviderUtil;
import com.intellij.ide.actions.SearchEverywherePsiRenderer;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.ide.util.gotoByName.*;
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo;
import com.intellij.ide.util.scopeChooser.ScopeDescriptor;
import com.intellij.lang.LangBundle;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.Application;
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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.OffsetIcon;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
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
  protected boolean myEverywhere;
  protected ScopeDescriptor myScopeDescriptor;

  private final GlobalSearchScope myEverywhereScope;
  private final GlobalSearchScope myProjectScope;

  protected final SmartPsiElementPointer<PsiElement> myPsiContext;

  protected AbstractGotoSEContributor(@NotNull AnActionEvent event) {
    myProject = event.getRequiredData(CommonDataKeys.PROJECT);
    PsiElement context = GotoActionBase.getPsiContext(event);
    myPsiContext = context != null ? SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(context) : null;
    myEverywhereScope = GlobalSearchScope.everythingScope(myProject);
    List<ScopeDescriptor> scopeDescriptors = createScopes();
    GlobalSearchScope projectScope = GlobalSearchScope.projectScope(myProject);
    if (myEverywhereScope.equals(projectScope)) {
      // just get the second scope, i.e. Attached Directories in DataGrip
      ScopeDescriptor secondScope = JBIterable.from(scopeDescriptors)
        .filter(o -> !o.scopeEquals(myEverywhereScope) && !o.scopeEquals(null))
        .first();
      projectScope = secondScope != null ? (GlobalSearchScope)secondScope.getScope() : myEverywhereScope;
    }
    myProjectScope = projectScope;
    myScopeDescriptor = getInitialSelectedScope(scopeDescriptors);

    myProject.getMessageBus().connect(this).subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
      @Override
      public void pluginUnloaded(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        myScopeDescriptor = getInitialSelectedScope(createScopes());
      }
    });
  }

  private List<ScopeDescriptor> createScopes() {
    DataContext context = createContext();
    List<ScopeDescriptor> res = new ArrayList<>();
    ScopeChooserCombo.processScopes(
      myProject, context,
      ScopeChooserCombo.OPT_LIBRARIES | ScopeChooserCombo.OPT_EMPTY_SCOPES,
      new CommonProcessors.CollectProcessor<>(res));

    return res;
  }

  private DataContext createContext() {
    DataContext parentContext = myProject == null ? null : SimpleDataContext.getProjectContext(myProject);
    PsiElement context = myPsiContext != null ? myPsiContext.getElement() : null;
    PsiFile file = context == null ? null : context.getContainingFile();

    Map<String, Object> map = new HashMap<>();
    map.put(CommonDataKeys.PSI_ELEMENT.getName(), context);
    map.put(CommonDataKeys.PSI_FILE.getName(), file);
    return SimpleDataContext.getSimpleContext(map, parentContext);
  }

  @Nullable
  @Override
  public String getAdvertisement() {
    return DumbService.isDumb(myProject) ? "Results might be incomplete. The project is being indexed." : null;
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
        boolean processScopes(@NotNull Processor<? super ScopeDescriptor> processor) {
          return ContainerUtil.process(createScopes(), processor);
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
    result.add(new SearchEverywhereUIBase.FiltersAction(filter, onChanged));
    return result;
  }

  @NotNull
  private ScopeDescriptor getInitialSelectedScope(List<ScopeDescriptor> scopeDescriptors) {
    String selectedScope = myProject == null ? null : getSelectedScopes(myProject).get(getClass().getSimpleName());
    if (Registry.is("search.everywhere.show.scopes") && Registry.is("search.everywhere.sticky.scopes") &&
        StringUtil.isNotEmpty(selectedScope)) {
      for (ScopeDescriptor descriptor : scopeDescriptors) {
        if (!selectedScope.equals(descriptor.getDisplayName()) || descriptor.scopeEquals(null)) continue;
        return descriptor;
      }
    }
    return new ScopeDescriptor(myProjectScope);
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

    Runnable fetchRunnable = () -> {
      if (!isDumbAware() && DumbService.isDumb(myProject)) return;

      FilteringGotoByModel<?> model = createModel(myProject);
      if (progressIndicator.isCanceled()) return;

      PsiElement context = myPsiContext != null ? myPsiContext.getElement() : null;
      ChooseByNameItemProvider provider = ChooseByNameModelEx.getItemProvider(model, context);
      GlobalSearchScope scope = Registry.is("search.everywhere.show.scopes")
                                ? (GlobalSearchScope)Objects.requireNonNull(myScopeDescriptor.getScope())
                                : null;

      boolean everywhere = scope == null ? myEverywhere : scope.isSearchInLibraries();
      ChooseByNameViewModel viewModel = new MyViewModel(myProject, model);

      if (scope != null && provider instanceof ChooseByNameInScopeItemProvider) {
        FindSymbolParameters parameters = FindSymbolParameters.wrap(pattern, scope);
        ((ChooseByNameInScopeItemProvider)provider).filterElementsWithWeights(viewModel, parameters, progressIndicator,
                                                                              item -> processElement(progressIndicator, consumer, model,
                                                                                                     item.getItem(), item.getWeight())
        );
      }
      else if (provider instanceof ChooseByNameWeightedItemProvider) {
        ((ChooseByNameWeightedItemProvider)provider).filterElementsWithWeights(viewModel, pattern, everywhere, progressIndicator,
                                                                               item -> processElement(progressIndicator, consumer, model,
                                                                                                      item.getItem(), item.getWeight())
        );
      }
      else {
        provider.filterElements(viewModel, pattern, everywhere, progressIndicator,
                                element -> processElement(progressIndicator, consumer, model, element,
                                                          getElementPriority(element, pattern))
        );
      }
    };


    Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode() && application.isDispatchThread()) {
      fetchRunnable.run();
    }
    else {
      ProgressIndicatorUtils.yieldToPendingWriteActions();
      ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(fetchRunnable, progressIndicator);
    }
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

    abstract boolean processScopes(@NotNull Processor<? super ScopeDescriptor> processor);

    @Override public boolean canBePerformed(@NotNull DataContext context) { return true; }
    @Override public boolean isPopup() { return true; }
    @Override public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) { return EMPTY_ARRAY; }

    @NotNull @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
      JComponent component = new ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
      ComponentUtil.putClientProperty(component, MnemonicHelper.MNEMONIC_CHECKER, keyCode ->
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
      e.getPresentation().setDescription(LangBundle.message("action.choose.scope.p.toggle.scope.description", shortcutText, shortcutText2));
      JComponent button = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
      if (button != null) {
        button.setBackground(selection.getColor());
      }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      JComponent button = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
      if (button == null || !button.isValid()) return;
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
      JList<ScopeDescriptor> fakeList = new JBList<>();
      processScopes(o -> {
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
      step.setDefaultOptionIndex(ContainerUtil.indexOf(items, o -> Objects.equals(o.getDisplayName(), selection.getDisplayName())));
      ListPopupImpl popup = new ListPopupImpl(e.getProject(), step);
      popup.setMaxRowCount(10);
      //noinspection unchecked
      popup.getList().setCellRenderer(renderer);
      popup.showUnderneathOf(button);
    }
  }

  private static class MyViewModel implements ChooseByNameViewModel {
    private final Project myProject;
    private final ChooseByNameModel myModel;

    private MyViewModel(Project project, ChooseByNameModel model) {
      myProject = project;
      myModel = model;
    }

    @Override
    public Project getProject() {
      return myProject;
    }

    @Override
    public @NotNull ChooseByNameModel getModel() {
      return myModel;
    }

    @Override
    public boolean isSearchInAnyPlace() {
      return myModel.useMiddleMatching();
    }

    @Override
    public @NotNull String transformPattern(@NotNull String pattern) {
      return ChooseByNamePopup.getTransformedPattern(pattern, myModel);
    }

    @Override
    public boolean canShowListForEmptyPattern() {
      return false;
    }

    @Override
    public int getMaximumListSizeLimit() {
      return 0;
    }
  }
}
