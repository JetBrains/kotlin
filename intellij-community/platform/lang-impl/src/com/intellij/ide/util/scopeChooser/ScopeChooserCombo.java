// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.scopeChooser;

import com.intellij.ide.DataManager;
import com.intellij.ide.util.treeView.WeighedItem;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.PredefinedSearchScopeProvider;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.SearchScopeProvider;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBEmptyBorder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.List;

public class ScopeChooserCombo extends ComboboxWithBrowseButton implements Disposable {
  private Project myProject;
  private boolean mySuggestSearchInLibs;
  private boolean myPrevSearchFiles;
  private boolean myCurrentSelection = true;
  private boolean myUsageView = true;
  private Condition<? super ScopeDescriptor> myScopeFilter;
  private boolean myShowEmptyScopes;
  private BrowseListener myBrowseListener = null;

  public ScopeChooserCombo() {
    super(new MyComboBox());
  }

  public ScopeChooserCombo(final Project project, boolean suggestSearchInLibs, boolean prevSearchWholeFiles, String preselect) {
    this();
    init(project, suggestSearchInLibs, prevSearchWholeFiles,  preselect);
  }

  public void init(final Project project, final String preselect){
    init(project, false, true, preselect);
  }

  public void init(final Project project, final boolean suggestSearchInLibs, final boolean prevSearchWholeFiles, final String preselect) {
    init(project, suggestSearchInLibs, prevSearchWholeFiles, preselect, null);
  }

  public void init(final Project project,
                   final boolean suggestSearchInLibs,
                   final boolean prevSearchWholeFiles,
                   final Object selection,
                   @Nullable Condition<? super ScopeDescriptor> scopeFilter) {
    if (myProject != null) {
      throw new IllegalStateException("scope chooser combo already initialized");
    }
    mySuggestSearchInLibs = suggestSearchInLibs;
    myPrevSearchFiles = prevSearchWholeFiles;
    myProject = project;

    NamedScopesHolder.ScopeListener scopeListener = () -> {
      SearchScope selectedScope = getSelectedScope();
      rebuildModel();
      selectItem(selectedScope);
    };
    myScopeFilter = scopeFilter;
    NamedScopeManager.getInstance(project).addScopeListener(scopeListener, this);
    DependencyValidationManager.getInstance(project).addScopeListener(scopeListener, this);
    addActionListener(this::handleScopeChooserAction);

    ComboBox<ScopeDescriptor> combo = getComboBox();
    combo.setMinimumAndPreferredWidth(JBUI.scale(300));
    combo.setRenderer(new MyRenderer());
    combo.putClientProperty("ComboBox.jbPopup", true);
    combo.updateUI();

    rebuildModel();

    selectItem(selection);
    new ComboboxSpeedSearch(combo) {
      @Override
      protected String getElementText(Object element) {
        if (element instanceof ScopeDescriptor) {
          final ScopeDescriptor descriptor = (ScopeDescriptor)element;
          return descriptor.getDisplayName();
        }
        return null;
      }
    };
  }

  @Override
  public ComboBox<ScopeDescriptor> getComboBox() {
    //noinspection unchecked
    return (ComboBox<ScopeDescriptor>)super.getComboBox();
  }

  public void setBrowseListener(BrowseListener browseListener) {
    myBrowseListener = browseListener;
  }

  public void setCurrentSelection(boolean currentSelection) {
    myCurrentSelection = currentSelection;
  }

  public void setUsageView(boolean usageView) {
    myUsageView = usageView;
  }

  public void selectItem(@Nullable Object selection) {
    if (selection == null) return;
    JComboBox combo = getComboBox();
    DefaultComboBoxModel model = (DefaultComboBoxModel)combo.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      ScopeDescriptor descriptor = (ScopeDescriptor)model.getElementAt(i);
      if (selection instanceof String && selection.equals(descriptor.getDisplayName()) ||
          selection instanceof SearchScope && descriptor.scopeEquals((SearchScope)selection)) {
        combo.setSelectedIndex(i);
        break;
      }
    }
  }

  /** @noinspection unused*/
  private void handleScopeChooserAction(ActionEvent ignore) {
    String selection = getSelectedScopeName();
    if (myBrowseListener != null) myBrowseListener.onBeforeBrowseStarted();
    EditScopesDialog dlg = EditScopesDialog.showDialog(myProject, selection);
    if (dlg.isOK()){
      rebuildModel();
      NamedScope namedScope = dlg.getSelectedScope();
      if (namedScope != null) {
        selectItem(namedScope.getName());
      }
    }
    if (myBrowseListener != null) myBrowseListener.onAfterBrowseFinished();
  }

  private void rebuildModel() {
    getComboBox().setModel(createModel());
  }

  @NotNull
  private DefaultComboBoxModel<ScopeDescriptor> createModel() {
    DefaultComboBoxModel<ScopeDescriptor> model = new DefaultComboBoxModel<>();
    createPredefinedScopeDescriptors(model);

    Comparator<SearchScope> comparator = (o1, o2) -> {
      int w1 = o1 instanceof WeighedItem ? ((WeighedItem)o1).getWeight() : Integer.MAX_VALUE;
      int w2 = o2 instanceof WeighedItem ? ((WeighedItem)o2).getWeight() : Integer.MAX_VALUE;
      if (w1 == w2) return StringUtil.naturalCompare(o1.getDisplayName(), o2.getDisplayName());
      return w1 - w2;
    };
    for (SearchScopeProvider each : SearchScopeProvider.EP_NAME.getExtensions()) {
      if (StringUtil.isEmpty(each.getDisplayName())) continue;
      List<SearchScope> scopes = each.getSearchScopes(myProject);
      if (scopes.isEmpty()) continue;
      model.addElement(new ScopeSeparator(each.getDisplayName()));
      for (SearchScope scope : ContainerUtil.sorted(scopes, comparator)) {
        model.addElement(new ScopeDescriptor(scope));
      }
    }
    return model;
  }

  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    Dimension preferredSize = super.getPreferredSize();
    return new Dimension(Math.min(400, preferredSize.width), preferredSize.height);
  }

  @Override
  public Dimension getMinimumSize() {
    if (isMinimumSizeSet()) {
      return super.getMinimumSize();
    }
    Dimension minimumSize = super.getMinimumSize();
    return new Dimension(Math.min(200, minimumSize.width), minimumSize.height);
  }

  private void createPredefinedScopeDescriptors(@NotNull DefaultComboBoxModel<ScopeDescriptor> model) {
    @SuppressWarnings("deprecation") final DataContext context = DataManager.getInstance().getDataContext();
    for (SearchScope scope : PredefinedSearchScopeProvider.getInstance().getPredefinedScopes(myProject, context, mySuggestSearchInLibs,
                                                                                             myPrevSearchFiles, myCurrentSelection,
                                                                                             myUsageView, myShowEmptyScopes)) {
      addScopeDescriptor(model, new ScopeDescriptor(scope));
    }
    for (ScopeDescriptorProvider provider : ScopeDescriptorProvider.EP_NAME.getExtensionList()) {
      for (ScopeDescriptor scopeDescriptor : provider.getScopeDescriptors(myProject)) {
        if(myScopeFilter == null || myScopeFilter.value(scopeDescriptor)) {
          model.addElement(scopeDescriptor);
        }
      }
    }
  }

  private void addScopeDescriptor(DefaultComboBoxModel<ScopeDescriptor> model, ScopeDescriptor scopeDescriptor) {
    if (myScopeFilter == null || myScopeFilter.value(scopeDescriptor)) {
      model.addElement(scopeDescriptor);
    }
  }

  public void setShowEmptyScopes(boolean showEmptyScopes) {
    myShowEmptyScopes = showEmptyScopes;
  }

  @Nullable
  public SearchScope getSelectedScope() {
    ScopeDescriptor item = (ScopeDescriptor)getComboBox().getSelectedItem();
    return item == null ? null : item.getScope();
  }

  @Nullable
  public String getSelectedScopeName() {
    ScopeDescriptor item = (ScopeDescriptor)getComboBox().getSelectedItem();
    return item == null ? null : item.getDisplayName();
  }

  private static class ScopeSeparator extends ScopeDescriptor {
    private final String myText;

    ScopeSeparator(@NotNull String text) {
      super(null);
      myText = text;
    }

    @Override
    public String getDisplayName() {
      return myText;
    }
  }

  private static class MyRenderer extends ColoredListCellRenderer<ScopeDescriptor> {
    final TitledSeparator separator = new TitledSeparator();

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends ScopeDescriptor> list,
                                         ScopeDescriptor value,
                                         int index,
                                         boolean selected,
                                         boolean hasFocus) {
      if (value == null) return;
      setIcon(value.getIcon());
      append(value.getDisplayName());
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ScopeDescriptor> list,
                                                  ScopeDescriptor value,
                                                  int index,
                                                  boolean selected,
                                                  boolean hasFocus) {
      if (value instanceof ScopeSeparator) {
        separator.setText(value.getDisplayName());
        separator.setBorder(index == -1 ? null : new JBEmptyBorder(UIUtil.DEFAULT_VGAP, 2, UIUtil.DEFAULT_VGAP, 0));
        return separator;
      }
      super.getListCellRendererComponent(list, value, index, selected, hasFocus);
      setIpad(index == -1 ? JBUI.emptyInsets() : JBUI.insets(1, UIUtil.LARGE_VGAP + 2, 1, 0));
      return this;
    }
  }

  public interface BrowseListener {
    void onBeforeBrowseStarted();
    void onAfterBrowseFinished();
  }

  private static class MyComboBox extends ComboBox {

    @Override
    public void setSelectedItem(Object item) {
      if (!(item instanceof ScopeSeparator)) {
        super.setSelectedItem(item);
      }
    }

    @Override
    public void setSelectedIndex(final int anIndex) {
      Object item = getItemAt(anIndex);
      if (!(item instanceof ScopeSeparator)) {
        super.setSelectedIndex(anIndex);
      }
    }
  }
}
