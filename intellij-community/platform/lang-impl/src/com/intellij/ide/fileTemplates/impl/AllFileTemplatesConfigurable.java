// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.fileTemplates.impl;

import com.intellij.CommonBundle;
import com.intellij.application.options.schemes.AbstractSchemeActions;
import com.intellij.application.options.schemes.SchemesModel;
import com.intellij.application.options.schemes.SimpleSchemesPanel;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.fileTemplates.*;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.IdeLanguageCustomization;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.options.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

public final class AllFileTemplatesConfigurable implements SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll,
                                                     Configurable.VariableProjectAppLevel {
  private static final Logger LOG = Logger.getInstance(AllFileTemplatesConfigurable.class);

  private static final String TEMPLATES_TITLE = IdeBundle.message("tab.filetemplates.templates");
  private static final String INCLUDES_TITLE = IdeBundle.message("tab.filetemplates.includes");
  private static final String CODE_TITLE = IdeBundle.message("tab.filetemplates.code");
  private static final String OTHER_TITLE = IdeBundle.message("tab.filetemplates.j2ee");

  final static class Provider extends ConfigurableProvider {
    private final Project myProject;

    Provider(@NotNull Project project) {
      myProject = project;
    }

    @NotNull
    @Override
    public Configurable createConfigurable() {
      return new AllFileTemplatesConfigurable(myProject);
    }
  }

  private final Project myProject;
  private final FileTemplateManager myManager;
  private JPanel myMainPanel;
  private FileTemplateTab myCurrentTab;
  private FileTemplateTab myTemplatesList;
  private FileTemplateTab myIncludesList;
  private FileTemplateTab myCodeTemplatesList;
  @Nullable
  private FileTemplateTab myOtherTemplatesList;
  private JComponent myToolBar;
  private TabbedPaneWrapper myTabbedPane;
  private FileTemplateConfigurable myEditor;
  private boolean myModified;
  private JComponent myEditorComponent;
  private JPanel myLeftPanel;
  private FileTemplateTab[] myTabs;
  private Disposable myUIDisposable;
  private final Set<String> myInternalTemplateNames;

  private FileTemplatesScheme myScheme;
  private final Map<FileTemplatesScheme, Map<String, FileTemplate[]>> myChangesCache = new HashMap<>();

  private static final String CURRENT_TAB = "FileTemplates.CurrentTab";
  private static final String SELECTED_TEMPLATE = "FileTemplates.SelectedTemplate";

  public AllFileTemplatesConfigurable(Project project) {
    myProject = project;
    myManager = FileTemplateManager.getInstance(project);
    myScheme = myManager.getCurrentScheme();
    myInternalTemplateNames = ContainerUtil.map2Set(myManager.getInternalTemplates(), FileTemplate::getName);
  }

  private void onRemove() {
    myCurrentTab.removeSelected();
    myModified = true;
  }

  private void onAdd() {
    String ext = JBIterable.from(IdeLanguageCustomization.getInstance().getPrimaryIdeLanguages())
      .filterMap(Language::getAssociatedFileType)
      .filterMap(FileType::getDefaultExtension)
      .first();
    createTemplate(IdeBundle.message("template.unnamed"), StringUtil.notNullize(ext, "txt"), "");
  }

  @NotNull
  FileTemplate createTemplate(@NotNull final String prefName, @NotNull final String extension, @NotNull final String content) {
    final FileTemplate[] templates = myCurrentTab.getTemplates();
    final FileTemplate newTemplate = FileTemplateUtil.createTemplate(prefName, extension, content, templates);
    myCurrentTab.addTemplate(newTemplate);
    myModified = true;
    myCurrentTab.selectTemplate(newTemplate);
    fireListChanged();
    myEditor.focusToNameField();
    return newTemplate;
  }

  private void onClone() {
    try {
      myEditor.apply();
    }
    catch (ConfigurationException ignore) {
    }

    final FileTemplate selected = myCurrentTab.getSelectedTemplate();
    if (selected == null) {
      return;
    }

    final FileTemplate[] templates = myCurrentTab.getTemplates();
    final Set<String> names = new HashSet<>();
    for (FileTemplate template : templates) {
      names.add(template.getName());
    }
    @SuppressWarnings("UnresolvedPropertyKey")
    final String nameTemplate = IdeBundle.message("template.copy.N.of.T");
    String name = MessageFormat.format(nameTemplate, "", selected.getName());
    int i = 0;
    while (names.contains(name)) {
      name = MessageFormat.format(nameTemplate, ++i + " ", selected.getName());
    }
    final FileTemplate newTemplate = new CustomFileTemplate(name, selected.getExtension());
    newTemplate.setText(selected.getText());
    newTemplate.setReformatCode(selected.isReformatCode());
    newTemplate.setLiveTemplateEnabled(selected.isLiveTemplateEnabled());
    myCurrentTab.addTemplate(newTemplate);
    myModified = true;
    myCurrentTab.selectTemplate(newTemplate);
    fireListChanged();
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("title.file.templates");
  }

  @Override
  public String getHelpTopic() {
    int index = myTabbedPane.getSelectedIndex();
    switch (index) {
      case 0:
        return "fileTemplates.templates";
      case 1:
        return "fileTemplates.includes";
      case 2:
        return "fileTemplates.code";
      case 3:
        return "fileTemplates.j2ee";
      default:
        throw new IllegalStateException("wrong index: " + index);
    }
  }

  @Override
  public JComponent createComponent() {
    myUIDisposable = Disposer.newDisposable();

    myTemplatesList = new FileTemplateTabAsList(TEMPLATES_TITLE) {
      @Override
      public void onTemplateSelected() {
        onListSelectionChanged();
      }
    };
    myIncludesList = new FileTemplateTabAsList(INCLUDES_TITLE) {
      @Override
      public void onTemplateSelected() {
        onListSelectionChanged();
      }
    };
    myCodeTemplatesList = new FileTemplateTabAsList(CODE_TITLE) {
      @Override
      public void onTemplateSelected() {
        onListSelectionChanged();
      }
    };
    myCurrentTab = myTemplatesList;

    final List<FileTemplateTab> allTabs = new ArrayList<>(Arrays.asList(myTemplatesList, myIncludesList, myCodeTemplatesList));

    final List<FileTemplateGroupDescriptorFactory> factories = FileTemplateGroupDescriptorFactory.EXTENSION_POINT_NAME.getExtensionList();
    if (!factories.isEmpty()) {
      myOtherTemplatesList = new FileTemplateTabAsTree(OTHER_TITLE) {
        @Override
        public void onTemplateSelected() {
          onListSelectionChanged();
        }

        @Override
        protected FileTemplateNode initModel() {
          SortedSet<FileTemplateGroupDescriptor> categories =
            new TreeSet<>(Comparator.comparing(FileTemplateGroupDescriptor::getTitle));


          for (FileTemplateGroupDescriptorFactory templateGroupFactory : factories) {
            ContainerUtil.addIfNotNull(categories, templateGroupFactory.getFileTemplatesDescriptor());
          }

          //noinspection HardCodedStringLiteral
          return new FileTemplateNode("ROOT", null,
                                      ContainerUtil.map2List(categories, FileTemplateNode::new));
        }
      };
      allTabs.add(myOtherTemplatesList);
    }

    myEditor = new FileTemplateConfigurable(myProject);
    myEditor.addChangeListener(__ -> onEditorChanged());
    myEditorComponent = myEditor.createComponent();
    myEditorComponent.setBorder(JBUI.Borders.empty(10, 0, 10, 10));

    myTabs = allTabs.toArray(new FileTemplateTab[0]);
    myTabbedPane = new TabbedPaneWrapper(myUIDisposable);
    myTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    myLeftPanel = new JPanel(new CardLayout());
    for (FileTemplateTab tab : myTabs) {
      myLeftPanel.add(ScrollPaneFactory.createScrollPane(tab.getComponent()), tab.getTitle());
      JPanel fakePanel = new JPanel();
      fakePanel.setPreferredSize(new Dimension(0, 0));
      myTabbedPane.addTab(tab.getTitle(), fakePanel);
    }

    myTabbedPane.addChangeListener(__ -> onTabChanged());

    DefaultActionGroup group = new DefaultActionGroup();
    AnAction removeAction = new DumbAwareAction(IdeBundle.message("action.remove.template"), null, AllIcons.General.Remove) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        onRemove();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        if (myCurrentTab == null) {
          e.getPresentation().setEnabled(false);
          return;
        }
        FileTemplate selectedItem = myCurrentTab.getSelectedTemplate();
        e.getPresentation().setEnabled(selectedItem != null && !isInternalTemplate(selectedItem.getName(), myCurrentTab.getTitle()));
      }
    };
    AnAction addAction = new DumbAwareAction(IdeBundle.message("action.create.template"), null, AllIcons.General.Add) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        onAdd();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!(myCurrentTab == myCodeTemplatesList || myCurrentTab == myOtherTemplatesList));
      }
    };
    AnAction cloneAction = new DumbAwareAction(IdeBundle.message("action.copy.template"), null, PlatformIcons.COPY_ICON) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        onClone();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(myCurrentTab != myCodeTemplatesList
                                       && myCurrentTab != myOtherTemplatesList
                                       && myCurrentTab.getSelectedTemplate() != null);
      }
    };
    AnAction resetAction = new DumbAwareAction(IdeBundle.message("action.reset.to.default"), null, AllIcons.Actions.Rollback) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        onReset();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        if (myCurrentTab == null) {
          e.getPresentation().setEnabled(false);
          return;
        }
        final FileTemplate selectedItem = myCurrentTab.getSelectedTemplate();
        e.getPresentation().setEnabled(selectedItem instanceof BundledFileTemplate && !selectedItem.isDefault());
      }
    };
    group.add(addAction);
    group.add(removeAction);
    group.add(cloneAction);
    group.add(resetAction);

    addAction.registerCustomShortcutSet(CommonShortcuts.INSERT, myCurrentTab.getComponent());
    removeAction.registerCustomShortcutSet(CommonShortcuts.getDelete(),
                                           myCurrentTab.getComponent());

    myToolBar = ActionManager.getInstance().createActionToolbar("FileTemplatesConfigurable", group, true).getComponent();
    myToolBar.setBorder(new CustomLineBorder(1, 1, 0, 1));

    SchemesPanel schemesPanel = new SchemesPanel();
    schemesPanel.setBorder(JBUI.Borders.empty(5, 10, 0, 10));
    schemesPanel.resetSchemes(Arrays.asList(FileTemplatesScheme.DEFAULT, myManager.getProjectScheme()));

    JPanel leftPanelWrapper = new JPanel(new BorderLayout());
    leftPanelWrapper.setBorder(JBUI.Borders.empty(10, 10, 10, 0));
    leftPanelWrapper.add(BorderLayout.NORTH, myToolBar);
    leftPanelWrapper.add(BorderLayout.CENTER, myLeftPanel);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(myTabbedPane.getComponent(), BorderLayout.NORTH);
    Splitter splitter = new Splitter(false, 0.3f);
    splitter.setDividerWidth(JBUIScale.scale(10));
    splitter.setFirstComponent(leftPanelWrapper);
    splitter.setSecondComponent(myEditorComponent);
    centerPanel.add(splitter, BorderLayout.CENTER);

    myMainPanel = new JPanel(new BorderLayout());
    myMainPanel.add(schemesPanel, BorderLayout.NORTH);
    myMainPanel.add(centerPanel, BorderLayout.CENTER);

    final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    final String tabName = propertiesComponent.getValue(CURRENT_TAB);
    selectTab(tabName);

    return myMainPanel;
  }

  private void onReset() {
    FileTemplate selected = myCurrentTab.getSelectedTemplate();
    if (selected instanceof BundledFileTemplate) {
      if (Messages.showOkCancelDialog(IdeBundle.message("prompt.reset.to.original.template"),
                                      IdeBundle.message("title.reset.template"), "Reset", CommonBundle.getCancelButtonText(), Messages.getQuestionIcon()) !=
          Messages.OK) {
        return;
      }
      ((BundledFileTemplate)selected).revertToDefaults();
      myEditor.reset();
      myModified = true;
    }
  }

  private void onEditorChanged() {
    fireListChanged();
  }

  private void onTabChanged() {
    applyEditor(myCurrentTab.getSelectedTemplate());

    FileTemplateTab tab = myCurrentTab;
    final int selectedIndex = myTabbedPane.getSelectedIndex();
    if (0 <= selectedIndex && selectedIndex < myTabs.length) {
      myCurrentTab = myTabs[selectedIndex];
    }
    ((CardLayout)myLeftPanel.getLayout()).show(myLeftPanel, myCurrentTab.getTitle());
    onListSelectionChanged();
    // request focus to a list (or tree) later to avoid moving focus to the tabbed pane
    if (tab != myCurrentTab) EventQueue.invokeLater(myCurrentTab.getComponent()::requestFocus);
  }

  private void onListSelectionChanged() {
    FileTemplate selectedValue = myCurrentTab.getSelectedTemplate();
    FileTemplate prevTemplate = myEditor == null ? null : myEditor.getTemplate();
    if (prevTemplate != selectedValue) {
      LOG.assertTrue(myEditor != null, "selected:" + selectedValue + "; prev:" + prevTemplate);
      //selection has changed
      if (Arrays.asList(myCurrentTab.getTemplates()).contains(prevTemplate) && !applyEditor(prevTemplate)) {
        return;
      }
      if (selectedValue == null) {
        myEditor.setTemplate(null, FileTemplateManagerImpl.getInstanceImpl(myProject).getDefaultTemplateDescription());
        myEditorComponent.repaint();
      }
      else {
        selectTemplate(selectedValue);
      }
    }
  }

  private boolean applyEditor(FileTemplate prevTemplate) {
    if (myEditor.isModified()) {
      try {
        myModified = true;
        myEditor.apply();
        fireListChanged();
      }
      catch (ConfigurationException e) {
        if (Arrays.asList(myCurrentTab.getTemplates()).contains(prevTemplate)) {
          myCurrentTab.selectTemplate(prevTemplate);
        }
        Messages.showErrorDialog(myMainPanel, e.getMessage(), IdeBundle.message("title.cannot.save.current.template"));
        return false;
      }
    }
    return true;
  }

  private void selectTemplate(FileTemplate template) {
    URL defDesc = null;
    if (myCurrentTab == myTemplatesList) {
      defDesc = FileTemplateManagerImpl.getInstanceImpl(myProject).getDefaultTemplateDescription();
    }
    else if (myCurrentTab == myIncludesList) {
      defDesc = FileTemplateManagerImpl.getInstanceImpl(myProject).getDefaultIncludeDescription();
    }
    if (myEditor.getTemplate() != template) {
      myEditor.setTemplate(template, defDesc);
      final boolean isInternal = template != null && isInternalTemplate(template.getName(), myCurrentTab.getTitle());
      myEditor.setShowInternalMessage(isInternal ? " " : null);
      myEditor.setShowAdjustCheckBox(myTemplatesList == myCurrentTab);
    }
  }

  @Override
  public boolean isProjectLevel() {
    return myScheme != null && myScheme != FileTemplatesScheme.DEFAULT && !myScheme.getProject().isDefault();
  }

  // internal template could not be removed and should be rendered bold
  static boolean isInternalTemplate(String templateName, String templateTabTitle) {
    if (templateName == null) {
      return false;
    }
    if (Comparing.strEqual(templateTabTitle, TEMPLATES_TITLE)) {
      return isInternalTemplateName(templateName);
    }
    if (Comparing.strEqual(templateTabTitle, CODE_TITLE)) {
      return true;
    }
    if (Comparing.strEqual(templateTabTitle, OTHER_TITLE)) {
      return true;
    }
    if (Comparing.strEqual(templateTabTitle, INCLUDES_TITLE)) {
      return Comparing.strEqual(templateName, FileTemplateManager.FILE_HEADER_TEMPLATE_NAME);
    }
    return false;
  }

  private static boolean isInternalTemplateName(final String templateName) {
    for(InternalTemplateBean bean: InternalTemplateBean.EP_NAME.getExtensionList()) {
      if (Comparing.strEqual(templateName, bean.name)) {
        return true;
      }
    }
    return false;
  }

  private void initLists() {
    FileTemplatesScheme scheme = myManager.getCurrentScheme();
    myManager.setCurrentScheme(myScheme);
    myTemplatesList.init(getTemplates(FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY));
    myIncludesList.init(getTemplates(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY));
    myCodeTemplatesList.init(getTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY));
    myTabbedPane.setEnabledAt(2, !myCodeTemplatesList.myTemplates.isEmpty());
    if (myOtherTemplatesList != null) {
      myOtherTemplatesList.init(getTemplates(FileTemplateManager.J2EE_TEMPLATES_CATEGORY));
    }
    myManager.setCurrentScheme(scheme);
  }

  private FileTemplate[] getTemplates(String category) {
    Map<String, FileTemplate[]> templates = myChangesCache.get(myScheme);
    if (templates == null) {
      return myManager.getTemplates(category);
    }
    return templates.get(category);
  }

  @Override
  public boolean isModified() {
    return myScheme != myManager.getCurrentScheme() || !myChangesCache.isEmpty() || isSchemeModified();
  }

  private boolean isSchemeModified() {
    return myModified || myEditor != null && myEditor.isModified();
  }

  private void checkCanApply(FileTemplateTab list) throws ConfigurationException {
    final FileTemplate[] templates = myCurrentTab.getTemplates();
    final List<String> allNames = new ArrayList<>();
    FileTemplate itemWithError = null;
    String errorString = null;
    for (FileTemplate template : templates) {
      final String currName = template.getName();
      if (currName.isEmpty()) {
        itemWithError = template;
        errorString = IdeBundle.message("error.please.specify.template.name");
        break;
      }
      if (allNames.contains(currName)) {
        itemWithError = template;
        errorString = "Template with name \'" + currName + "\' already exists. Please specify a different template name";
        break;
      }
      allNames.add(currName);
    }

    if (itemWithError != null) {
      myTabbedPane.setSelectedIndex(Arrays.asList(myTabs).indexOf(list));
      selectTemplate(itemWithError);
      list.selectTemplate(itemWithError);
      ApplicationManager.getApplication().invokeLater(myEditor::focusToNameField);
      throw new ConfigurationException(errorString);
    }
  }

  private void fireListChanged() {
    if (myCurrentTab != null) {
      myCurrentTab.fireDataChanged();
    }
    if (myMainPanel != null) {
      myMainPanel.revalidate();
    }
  }

  @Override
  public void apply() throws ConfigurationException {
    if (myEditor != null && myEditor.isModified()) {
      myModified = true;
      myEditor.apply();
    }

    for (FileTemplateTab list : myTabs) {
      checkCanApply(list);
    }
    updateCache();
    for (Map.Entry<FileTemplatesScheme, Map<String, FileTemplate[]>> entry : myChangesCache.entrySet()) {
      myManager.setCurrentScheme(entry.getKey());
      Map<String, FileTemplate[]> templates = entry.getValue();
      myManager.setTemplates(FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY, Arrays.asList(templates.get(FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY)));
      myManager.setTemplates(FileTemplateManager.INTERNAL_TEMPLATES_CATEGORY, Arrays.asList(templates.get(FileTemplateManager.INTERNAL_TEMPLATES_CATEGORY)));
      myManager.setTemplates(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY, Arrays.asList(templates.get(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY)));
      myManager.setTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY, Arrays.asList(templates.get(FileTemplateManager.CODE_TEMPLATES_CATEGORY)));
      myManager.setTemplates(FileTemplateManager.J2EE_TEMPLATES_CATEGORY, Arrays.asList(templates.get(FileTemplateManager.J2EE_TEMPLATES_CATEGORY)));
    }
    myChangesCache.clear();

    myManager.setCurrentScheme(myScheme);

    if (myEditor != null) {
      myModified = false;
      fireListChanged();
    }
  }

  private boolean selectTab(String tabName) {
    int idx = 0;
    for (FileTemplateTab tab : myTabs) {
      if (Comparing.strEqual(tab.getTitle(), tabName)) {
        myCurrentTab = tab;
        myTabbedPane.setSelectedIndex(idx);
        return true;
      }
      idx++;
    }
    return false;
  }

  @Override
  public void reset() {
    myEditor.reset();
    changeScheme(myManager.getCurrentScheme());
    myChangesCache.clear();
    myModified = false;
  }

  @Override
  public void disposeUIResources() {
    if (myCurrentTab != null) {
      final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
      propertiesComponent.setValue(CURRENT_TAB, myCurrentTab.getTitle(), TEMPLATES_TITLE);
      final FileTemplate template = myCurrentTab.getSelectedTemplate();
      if (template != null) {
        propertiesComponent.setValue(SELECTED_TEMPLATE, template.getName());
      }
    }

    if (myEditor != null) {
      myEditor.disposeUIResources();
      myEditor = null;
      myEditorComponent = null;
    }
    myMainPanel = null;
    if (myUIDisposable != null) {
      Disposer.dispose(myUIDisposable);
      myUIDisposable = null;
    }
    myTabbedPane = null;
    myToolBar = null;
    myTabs = null;
    myCurrentTab = null;
    myTemplatesList = null;
    myCodeTemplatesList = null;
    myIncludesList = null;
    myOtherTemplatesList = null;
  }

  @Override
  @NotNull
  public String getId() {
    return "fileTemplates";
  }

  public static void editCodeTemplate(@NotNull final String templateId, Project project) {
    final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
    final AllFileTemplatesConfigurable configurable = new AllFileTemplatesConfigurable(project);
    util.editConfigurable(project, configurable, () -> {
      configurable.myTabbedPane.setSelectedIndex(ArrayUtil.indexOf(configurable.myTabs, configurable.myCodeTemplatesList));
      for (FileTemplate template : configurable.myCodeTemplatesList.getTemplates()) {
        if (Comparing.equal(templateId, template.getName())) {
          configurable.myCodeTemplatesList.selectTemplate(template);
          break;
        }
      }
    });
  }

  void changeScheme(@NotNull FileTemplatesScheme scheme) {
    if (myEditor != null && myEditor.isModified()) {
      myModified = true;
      try {
        myEditor.apply();
      }
      catch (ConfigurationException e) {
        Messages.showErrorDialog(myEditorComponent, e.getMessage(), e.getTitle());
        return;
      }
    }
    updateCache();
    myScheme = scheme;
    initLists();
  }

  private void updateCache() {
    if (isSchemeModified()) {
      if (!myChangesCache.containsKey(myScheme)) {
        Map<String, FileTemplate[]> templates = new HashMap<>();
        FileTemplate[] allTemplates = myTemplatesList.getTemplates();
        templates.put(
          FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY,
          ContainerUtil.filter(allTemplates, template -> !myInternalTemplateNames.contains(template.getName()))
            .toArray(FileTemplate.EMPTY_ARRAY));
        templates.put(
          FileTemplateManager.INTERNAL_TEMPLATES_CATEGORY,
          ContainerUtil.filter(allTemplates, template -> myInternalTemplateNames.contains(template.getName()))
            .toArray(FileTemplate.EMPTY_ARRAY));
        templates.put(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY, myIncludesList.getTemplates());
        templates.put(FileTemplateManager.CODE_TEMPLATES_CATEGORY, myCodeTemplatesList.getTemplates());
        templates.put(
          FileTemplateManager.J2EE_TEMPLATES_CATEGORY,
          myOtherTemplatesList == null ? FileTemplate.EMPTY_ARRAY : myOtherTemplatesList.getTemplates());
        myChangesCache.put(myScheme, templates);
      }
    }
  }

  @NotNull
  public FileTemplateManager getManager() {
    return myManager;
  }

  @TestOnly
  FileTemplateConfigurable getEditor() {
    return myEditor;
  }

  @TestOnly
  FileTemplateTab[] getTabs() {
    return myTabs;
  }

  private final class SchemesPanel extends SimpleSchemesPanel<FileTemplatesScheme> implements SchemesModel<FileTemplatesScheme> {
    @NotNull
    @Override
    protected AbstractSchemeActions<FileTemplatesScheme> createSchemeActions() {
      return new AbstractSchemeActions<FileTemplatesScheme>(this) {
        @Override
        protected void resetScheme(@NotNull FileTemplatesScheme scheme) {
          throw new UnsupportedOperationException();
        }

        @Override
        protected void duplicateScheme(@NotNull FileTemplatesScheme scheme, @NotNull String newName) {
          throw new UnsupportedOperationException();
        }

        @Override
        protected void onSchemeChanged(@Nullable FileTemplatesScheme scheme) {
          if (scheme != null) changeScheme(scheme);
        }

        @Override
        protected void renameScheme(@NotNull FileTemplatesScheme scheme, @NotNull String newName) {
          throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        protected Class<FileTemplatesScheme> getSchemeType() {
          return FileTemplatesScheme.class;
        }
      };
    }

    @NotNull
    @Override
    public SchemesModel<FileTemplatesScheme> getModel() {
      return this;
    }

    @Override
    protected boolean supportsProjectSchemes() {
      return false;
    }

    @Override
    protected boolean highlightNonDefaultSchemes() {
      return false;
    }

    @Override
    public boolean useBoldForNonRemovableSchemes() {
      return true;
    }

    @Override
    public boolean canDuplicateScheme(@NotNull FileTemplatesScheme scheme) {
      return false;
    }

    @Override
    public boolean canResetScheme(@NotNull FileTemplatesScheme scheme) {
      return false;
    }

    @Override
    public boolean canDeleteScheme(@NotNull FileTemplatesScheme scheme) {
      return false;
    }

    @Override
    public boolean isProjectScheme(@NotNull FileTemplatesScheme scheme) {
      return false;
    }

    @Override
    public boolean canRenameScheme(@NotNull FileTemplatesScheme scheme) {
      return false;
    }

    @Override
    public boolean containsScheme(@NotNull String name, boolean projectScheme) {
      return false;
    }

    @Override
    public boolean differsFromDefault(@NotNull FileTemplatesScheme scheme) {
      return false;
    }

    @Override
    public void removeScheme(@NotNull FileTemplatesScheme scheme) {
      throw new UnsupportedOperationException();
    }
  }
}
