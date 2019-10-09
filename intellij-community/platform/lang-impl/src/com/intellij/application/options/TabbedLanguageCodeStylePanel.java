// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import com.intellij.application.options.codeStyle.CodeStyleBlankLinesPanel;
import com.intellij.application.options.codeStyle.CodeStyleSchemesModel;
import com.intellij.application.options.codeStyle.CodeStyleSpacesPanel;
import com.intellij.application.options.codeStyle.WrappingAndBracesPanel;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.codeStyle.*;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.GraphicsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Rustam Vishnyakov
 */

public abstract class TabbedLanguageCodeStylePanel extends CodeStyleAbstractPanel {
  private CodeStyleAbstractPanel myActiveTab;
  private List<CodeStyleAbstractPanel> myTabs;
  private JPanel myPanel;
  private TabbedPaneWrapper myTabbedPane;
  private final PredefinedCodeStyle[] myPredefinedCodeStyles;
  private JPopupMenu myCopyFromMenu;
  @Nullable private TabChangeListener myListener;
  private final EventDispatcher<PredefinedCodeStyleListener> myPredefinedCodeStyleEventDispatcher = EventDispatcher.create(PredefinedCodeStyleListener.class);

  private Ref<LanguageCodeStyleSettingsProvider> myProviderRef;

  protected TabbedLanguageCodeStylePanel(@Nullable Language language, CodeStyleSettings currentSettings, CodeStyleSettings settings) {
    super(language, currentSettings, settings);
    myPredefinedCodeStyles = getPredefinedStyles();
  }

  /**
   * Initializes all standard tabs: "Tabs and Indents", "Spaces", "Blank Lines" and "Wrapping and Braces" if relevant.
   * For "Tabs and Indents" LanguageCodeStyleSettingsProvider must instantiate its own indent options, for other standard tabs it
   * must return false in usesSharedPreview() method. You can override this method to add your own tabs by calling super.initTabs() and
   * then addTab() methods or selectively add needed tabs with your own implementation.
   * @param settings  Code style settings to be used with initialized panels.
   * @see LanguageCodeStyleSettingsProvider
   * @see #addIndentOptionsTab(CodeStyleSettings)
   * @see #addSpacesTab(CodeStyleSettings)
   * @see #addBlankLinesTab(CodeStyleSettings)
   * @see #addWrappingAndBracesTab(CodeStyleSettings)
   */
  protected void initTabs(CodeStyleSettings settings) {
    addIndentOptionsTab(settings);
    if (getProvider() != null) {
      addSpacesTab(settings);
      addWrappingAndBracesTab(settings);
      addBlankLinesTab(settings);
    }
  }

  /**
   * Adds "Tabs and Indents" tab if the language has its own LanguageCodeStyleSettings provider and instantiates indent options in
   * getDefaultSettings() method.
   * @param settings CodeStyleSettings to be used with "Tabs and Indents" panel.
   */
  protected void addIndentOptionsTab(CodeStyleSettings settings) {
    if (getProvider() != null) {
      IndentOptionsEditor indentOptionsEditor = getProvider().getIndentOptionsEditor();
      if (indentOptionsEditor != null) {
        MyIndentOptionsWrapper indentOptionsWrapper = new MyIndentOptionsWrapper(settings, indentOptionsEditor);
        addTab(indentOptionsWrapper);
      }
    }
  }

  protected void addSpacesTab(CodeStyleSettings settings) {
    addTab(new MySpacesPanel(settings));
  }

  protected void addBlankLinesTab(CodeStyleSettings settings) {
    addTab(new MyBlankLinesPanel(settings));
  }

  protected void addWrappingAndBracesTab(CodeStyleSettings settings) {
    addTab(new MyWrappingAndBracesPanel(settings));
  }

  protected void ensureTabs() {
    if (myTabs == null) {
      myPanel = new JPanel();
      myPanel.setLayout(new BorderLayout());
      myTabbedPane = new TabbedPaneWrapper(this);
      myTabbedPane.addChangeListener(__ -> {
        if (myListener != null) {
          String title = myTabbedPane.getSelectedTitle();
          if (title != null) {
            myListener.tabChanged(this, title);
          }
        }
      });
      myTabs = new ArrayList<>();
      myPanel.add(myTabbedPane.getComponent());
      initTabs(getSettings());
    }
    assert !myTabs.isEmpty();
  }

  public void showSetFrom(Component component) {
    initCopyFromMenu();
    DefaultActionGroup group = new DefaultActionGroup();
    for (Component c : myCopyFromMenu.getComponents()) {
      if (c instanceof JSeparator) {
        group.addSeparator();
      }
      else if (c instanceof JMenuItem) {
        group.add(new DumbAwareAction(((JMenuItem)c).getText(), "", ObjectUtils.notNull(((JMenuItem)c).getIcon(), EmptyIcon.ICON_16)) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            ((JMenuItem)c).doClick();
          }
        });
      }
    }
    int maxRows = group.getChildrenCount() > 17 ? 15 : -1;
    DataContext dataContext = DataManager.getInstance().getDataContext(component);
    JBPopupFactory.getInstance().createActionGroupPopup(
      null, group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false, null, maxRows, null, "popup@TabbedLanguageCodeStylePanel")
    .showUnderneathOf(component);
  }

  private void initCopyFromMenu() {
    if (myCopyFromMenu == null) {
      myCopyFromMenu = new JBPopupMenu();
      myCopyFromMenu.setFocusable(false);
      setupCopyFromMenu(myCopyFromMenu);
    }
  }

  /**
   * Adds a tab with the given CodeStyleAbstractPanel. Tab title is taken from getTabTitle() method.
   * @param tab The panel to use in a tab.
   */
  protected final void addTab(CodeStyleAbstractPanel tab) {
    myTabs.add(tab);
    tab.setShouldUpdatePreview(true);
    addPanelToWatch(tab.getPanel());
    myTabbedPane.addTab(tab.getTabTitle(), tab.getPanel());
    if (myActiveTab == null) {
      myActiveTab = tab;
    }
  }

  private void addTab(Configurable configurable) {
    ConfigurableWrapper wrapper = new ConfigurableWrapper(configurable, getSettings());
    addTab(wrapper);
  }

  /**
   * Creates and adds a tab from CodeStyleSettingsProvider. The provider may return false in hasSettingsPage() method in order not to be
   * shown at top level of code style settings.
   * @param provider The provider used to create a settings page.
   */
  protected final void createTab(CodeStyleSettingsProvider provider) {
    if (provider.hasSettingsPage()) return;
    Configurable configurable = provider.createConfigurable(getCurrentSettings(), getSettings());
    addTab(configurable);
  }

  @Override
  public final void setModel(@NotNull CodeStyleSchemesModel model) {
    super.setModel(model);
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.setModel(model);
    }
  }

  @Override
  protected int getRightMargin() {
    ensureTabs();
    return myActiveTab.getRightMargin();
  }

  @Override
  protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
    ensureTabs();
    return myActiveTab.createHighlighter(scheme);
  }

  @NotNull
  @Override
  protected FileType getFileType() {
    ensureTabs();
    return myActiveTab.getFileType();
  }

  @Override
  protected String getPreviewText() {
    ensureTabs();
    return myActiveTab.getPreviewText();
  }

  @Override
  protected void updatePreview(boolean useDefaultSample) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.updatePreview(useDefaultSample);
    }
  }

  @Override
  public void onSomethingChanged() {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.setShouldUpdatePreview(true);
      tab.onSomethingChanged();
    }
  }

  @Override
  public void apply(CodeStyleSettings settings) throws ConfigurationException {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.apply(settings);
    }
  }

  @Override
  public void dispose() {
    for (CodeStyleAbstractPanel tab : myTabs) {
      Disposer.dispose(tab);
    }
    super.dispose();
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      if (tab.isModified(settings)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  protected void resetImpl(CodeStyleSettings settings) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.resetImpl(settings);
    }
  }


  @Override
  public void setupCopyFromMenu(JPopupMenu copyMenu) {
    super.setupCopyFromMenu(copyMenu);
    if (myPredefinedCodeStyles.length > 0) {
      fillPredefinedStylesAndLanguages(copyMenu);
    }
    else {
      fillLanguages(copyMenu);
    }
  }

  private void fillPredefinedStylesAndLanguages(JPopupMenu copyMenu) {
    fillPredefinedStyles(copyMenu);
    LanguageCodeStyleSettingsProvider provider = getProvider();
    int n = 0;
    if (provider != null) n = provider.getApplicableLanguages().size();
    if (n > 0) {
      copyMenu.addSeparator();
      if (n <= 15) {
        fillLanguages(copyMenu);
      }
      else {
        JMenu langs = new JMenu(ApplicationBundle.message("code.style.set.from.menu.language")) {
          @Override
          public void paint(Graphics g) {
            GraphicsUtil.setupAntialiasing(g);
            super.paint(g);
          }
        };
        copyMenu.add(langs);
        fillLanguages(langs);
      }
    }
  }

  private void fillLanguages(JComponent parentMenu) {
    List<Language> languages = getProvider() != null ? getProvider().getApplicableLanguages() : Collections.emptyList();
    for (final Language lang : languages) {
      if (!lang.equals(getDefaultLanguage())) {
        final String langName = LanguageCodeStyleSettingsProvider.getLanguageName(lang);
        JMenuItem langItem = new JBMenuItem(langName);
        langItem.addActionListener(__ -> applyLanguageSettings(lang));
        parentMenu.add(langItem);
      }
    }
  }

  private void fillPredefinedStyles(JComponent parentMenu) {
    for (final PredefinedCodeStyle predefinedCodeStyle : myPredefinedCodeStyles) {
      JMenuItem predefinedItem = new JBMenuItem(predefinedCodeStyle.getName());
      parentMenu.add(predefinedItem);
      predefinedItem.addActionListener(__ -> applyPredefinedStyle(predefinedCodeStyle.getName()));
    }
  }

  protected void addPredefinedCodeStyleListener(@NotNull PredefinedCodeStyleListener listener) {
    myPredefinedCodeStyleEventDispatcher.addListener(listener, this);
  }

  private PredefinedCodeStyle[] getPredefinedStyles() {
    final Language language = getDefaultLanguage();
    final List<PredefinedCodeStyle> result = new ArrayList<>();

    for (PredefinedCodeStyle codeStyle : PredefinedCodeStyle.EP_NAME.getExtensions()) {
      if (language != null && codeStyle.isApplicableToLanguage(language)) {
        result.add(codeStyle);
      }
    }
    return result.toArray(PredefinedCodeStyle.EMPTY_ARRAY);
  }


  private void applyLanguageSettings(Language lang) {
    final Project currProject = ProjectUtil.guessCurrentProject(getPanel());
    CodeStyleSettings rootSettings = CodeStyle.getSettings(currProject);
    CodeStyleSettings targetSettings = getSettings();

    applyLanguageSettings(lang, rootSettings, targetSettings);
    reset(targetSettings);
    onSomethingChanged();
  }

  protected void applyLanguageSettings(Language lang, CodeStyleSettings rootSettings, CodeStyleSettings targetSettings) {
    CommonCodeStyleSettings sourceCommonSettings = rootSettings.getCommonSettings(lang);
    CommonCodeStyleSettings targetCommonSettings = targetSettings.getCommonSettings(getDefaultLanguage());
    targetCommonSettings.copyFrom(sourceCommonSettings);
  }

  private void applyPredefinedStyle(String styleName) {
    for (PredefinedCodeStyle style : myPredefinedCodeStyles) {
      if (style.getName().equals(styleName)) {
        applyPredefinedSettings(style);
        myPredefinedCodeStyleEventDispatcher.getMulticaster().styleApplied(style);
      }
    }
  }

  @Nullable
  private LanguageCodeStyleSettingsProvider getProvider() {
    if (myProviderRef == null) {
      myProviderRef = Ref.create(LanguageCodeStyleSettingsProvider.forLanguage(getDefaultLanguage()));
    }
    return myProviderRef.get();
  }

//========================================================================================================================================

  protected class MySpacesPanel extends CodeStyleSpacesPanel {

    public MySpacesPanel(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    protected boolean shouldHideOptions() {
      return true;
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }
  }

  protected class MyBlankLinesPanel extends CodeStyleBlankLinesPanel {

    public MyBlankLinesPanel(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }
  }

  protected class MyWrappingAndBracesPanel extends WrappingAndBracesPanel {

    public MyWrappingAndBracesPanel(CodeStyleSettings settings) {
      super(settings);
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }
  }


  //========================================================================================================================================

  private class ConfigurableWrapper extends CodeStyleAbstractPanel {

    private final Configurable myConfigurable;
    private JComponent myComponent;

    ConfigurableWrapper(@NotNull Configurable configurable, CodeStyleSettings settings) {
      super(settings);
      myConfigurable = configurable;

      Disposer.register(this, () -> myConfigurable.disposeUIResources());
    }

    @Override
    protected int getRightMargin() {
      return 0;
    }

    @Nullable
    @Override
    protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
      return null;
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    @Override
    protected FileType getFileType() {
      Language language = getDefaultLanguage();
      return language != null ? language.getAssociatedFileType() : FileTypes.PLAIN_TEXT;
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }

    @Override
    protected String getTabTitle() {
      return myConfigurable.getDisplayName();
    }

    @Override
    protected String getPreviewText() {
      return null;
    }

    @Override
    public void apply(CodeStyleSettings settings) throws ConfigurationException {
      if (myConfigurable instanceof CodeStyleConfigurable) {
        ((CodeStyleConfigurable)myConfigurable).apply(settings);
      }
      else {
        myConfigurable.apply();
      }
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
      return myConfigurable.isModified();
    }

    @Nullable
    @Override
    public JComponent getPanel() {
      if (myComponent == null) {
        myComponent = myConfigurable.createComponent();
      }
      return myComponent;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
      if (myConfigurable instanceof CodeStyleConfigurable) {
        ((CodeStyleConfigurable)myConfigurable).reset(settings);
      }
      else {
        myConfigurable.reset();
      }
    }
  }

  @NotNull
  @Override
  public OptionsContainingConfigurable getOptionIndexer() {
    return new OptionsContainingConfigurable() {
      @NotNull
      @Override
      public Set<String> processListOptions() {
        return Collections.emptySet();
      }

      @Override
      public Map<String, Set<String>> processListOptionsWithPaths() {
        final Map<String,Set<String>> result = new HashMap<>();
        for (CodeStyleAbstractPanel tab : myTabs) {
          result.put(tab.getTabTitle(), tab.processListOptions());
        }
        return result;
      }
    };
  }

  //========================================================================================================================================

  protected class MyIndentOptionsWrapper extends CodeStyleAbstractPanel {

    private final IndentOptionsEditor myEditor;
    private final JPanel myTopPanel = new JPanel(new BorderLayout());

    /**
     * @deprecated Use {@link #MyIndentOptionsWrapper(CodeStyleSettings, IndentOptionsEditor)}
     */
    @SuppressWarnings("unused")
    @Deprecated
    protected MyIndentOptionsWrapper(CodeStyleSettings settings, LanguageCodeStyleSettingsProvider provider, IndentOptionsEditor editor) {
      this(settings, editor);
    }

    protected MyIndentOptionsWrapper(CodeStyleSettings settings, IndentOptionsEditor editor) {
      super(settings);
      JPanel leftPanel = new JPanel(new BorderLayout());
      myTopPanel.add(leftPanel, BorderLayout.WEST);
      JPanel rightPanel = new JPanel();
      installPreviewPanel(rightPanel);
      myEditor = editor;
      if (myEditor != null) {
        JPanel panel = myEditor.createPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = ScrollPaneFactory.createScrollPane(panel, true);
        scroll.setPreferredSize(new Dimension(panel.getPreferredSize().width + scroll.getVerticalScrollBar().getPreferredSize().width + 5, -1));
        leftPanel.add(scroll, BorderLayout.CENTER);
      }
      myTopPanel.add(rightPanel, BorderLayout.CENTER);
    }

    @Override
    protected int getRightMargin() {
      return getProvider() != null ? getProvider().getRightMargin(LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS) : -1;
    }

    @Override
    protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
      return EditorHighlighterFactory.getInstance().createEditorHighlighter(getFileType(), scheme, null);
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    @Override
    protected FileType getFileType() {
      Language language = TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
      return language != null ? language.getAssociatedFileType() : FileTypes.PLAIN_TEXT;
    }

    @Override
    protected String getPreviewText() {
      return getProvider() != null ? getProvider().getCodeSample(LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS) : "";
    }

    @Override
    protected String getFileExt() {
      if (getProvider() != null) {
        String ext = getProvider().getFileExt();
        if (ext != null) return ext;
      }
      return super.getFileExt();
    }

    @Override
    public void apply(CodeStyleSettings settings) {
      CommonCodeStyleSettings.IndentOptions indentOptions = getIndentOptions(settings);
      if (indentOptions == null) return;
      myEditor.apply(settings, indentOptions);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
      CommonCodeStyleSettings.IndentOptions indentOptions = getIndentOptions(settings);
      if (indentOptions == null) return false;
      return myEditor.isModified(settings, indentOptions);
    }

    @Override
    public JComponent getPanel() {
      return myTopPanel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
      CommonCodeStyleSettings.IndentOptions indentOptions = getIndentOptions(settings);
      if (indentOptions == null && getProvider() != null) {
        myEditor.setEnabled(false);
        indentOptions = settings.getIndentOptions(getProvider().getLanguage().getAssociatedFileType());
      }
      assert indentOptions != null;
      myEditor.reset(settings, indentOptions);
    }

    @Nullable
    protected CommonCodeStyleSettings.IndentOptions getIndentOptions(CodeStyleSettings settings) {
      return settings.getCommonSettings(getDefaultLanguage()).getIndentOptions();
    }

    @Override
    public Language getDefaultLanguage() {
      return TabbedLanguageCodeStylePanel.this.getDefaultLanguage();
    }

    @Override
    protected String getTabTitle() {
      return ApplicationBundle.message("title.tabs.and.indents");
    }

    @Override
    public void onSomethingChanged() {
      super.onSomethingChanged();
      myEditor.setEnabled(true);
    }

  }

  @FunctionalInterface
  public interface TabChangeListener {
    void tabChanged(@NotNull TabbedLanguageCodeStylePanel source, @NotNull String tabTitle);
  }

  public void setListener(@Nullable TabChangeListener listener) {
    myListener = listener;
  }

  public void changeTab(@NotNull String tabTitle) {
    myTabbedPane.setSelectedTitle(tabTitle);
  }

  @Override
  public void highlightOptions(@NotNull String searchString) {
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.highlightOptions(searchString);
    }
  }
}
