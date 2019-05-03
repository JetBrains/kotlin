// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hints.InlayParameterHintsExtension;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.codeInsight.hints.Option;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.SwingActionLink;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.codeInsight.hints.HintUtilsKt.*;
import static com.intellij.openapi.editor.colors.CodeInsightColors.ERRORS_ATTRIBUTES;

public class ParameterNameHintsConfigurable extends DialogWrapper {
  private static final String LAST_EDITED_LANGUAGE_ID_KEY = "param.hints.settings.last.edited.language";

  private JPanel myConfigurable;
  private ComboBox<Language> myCurrentLanguageCombo;

  private Map<Language, EditorTextField> myEditors;
  private Map<Language, Boolean> myIsValidPatterns;
  private Map<Option, JBCheckBox> myOptions;

  private JPanel myPanel;
  private CardLayout myCardLayout;

  public ParameterNameHintsConfigurable() {
    this(null, null);
  }

  public ParameterNameHintsConfigurable(@Nullable Language selectedLanguage,
                                        @Nullable String newPreselectedPattern) {
    super(null);
    setTitle("Configure Parameter Name Hints");
    init();

    if (selectedLanguage != null) {
      selectedLanguage = getLanguageForSettingKey(selectedLanguage);
      showLanguagePanel(selectedLanguage);
      myCurrentLanguageCombo.setSelectedItem(selectedLanguage);
      if (newPreselectedPattern != null) {
        addSelectedText(selectedLanguage, newPreselectedPattern);
      }
    }
  }

  private void addSelectedText(@NotNull Language language, @NotNull String newPreselectedPattern) {
    EditorTextField textField = myEditors.get(language);

    String text = textField.getText();
    int startOffset = text.length();
    text += "\n" + newPreselectedPattern;
    int endOffset = text.length();

    textField.setText(text);
    textField.addSettingsProvider((editor) -> {
      SelectionModel model = editor.getSelectionModel();
      model.setSelection(startOffset + 1, endOffset);
    });
  }

  private void updateOkEnabled(@NotNull Language language, @NotNull EditorTextField editorTextField) {
    String text = editorTextField.getText();
    List<Integer> invalidLines = getBlackListInvalidLineNumbers(text);

    myIsValidPatterns.put(language, invalidLines.isEmpty());
    boolean isEveryOneValid = !myIsValidPatterns.containsValue(false);

    getOKAction().setEnabled(isEveryOneValid);

    Editor editor = editorTextField.getEditor();
    if (editor != null) {
      highlightErrorLines(invalidLines, editor);
    }
  }

  private static void highlightErrorLines(@NotNull List<Integer> lines, @NotNull Editor editor) {
    final TextAttributes attributes = editor.getColorsScheme().getAttributes(ERRORS_ATTRIBUTES);
    final Document document = editor.getDocument();
    final int totalLines = document.getLineCount();

    MarkupModel model = editor.getMarkupModel();
    model.removeAllHighlighters();
    lines.stream()
      .filter((current) -> current < totalLines)
      .forEach((line) -> model.addLineHighlighter(line, HighlighterLayer.ERROR, attributes));
  }

  @Override
  protected void doOKAction() {
    myEditors.forEach((language, editor) -> {
      String blacklist = editor.getText();
      storeBlackListDiff(language, blacklist);
    });

    myOptions.forEach((option, checkBox) -> option.set(checkBox.isSelected()));
    saveLastEditedLanguage();
    ParameterHintsPassFactory.forceHintsUpdateOnNextPass();

    super.doOKAction();
  }

  @Override
  public void doCancelAction() {
    saveLastEditedLanguage();

    super.doCancelAction();
  }

  private void saveLastEditedLanguage() {
    Arrays.stream(myPanel.getComponents())
      .filter(Component::isVisible)
      .findFirst()
      .ifPresent(component -> saveLastEditedLanguage(component.getName()));
  }

  private static void storeBlackListDiff(@NotNull Language language, @NotNull String text) {
    Set<String> updatedBlackList = StringUtil
      .split(text, "\n")
      .stream()
      .filter((e) -> !e.trim().isEmpty())
      .collect(Collectors.toCollection(LinkedHashSet::new));

    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    Set<String> defaultBlackList = provider.getDefaultBlackList();
    Diff diff = Diff.Builder.build(defaultBlackList, updatedBlackList);
    ParameterNameHintsSettings.getInstance().setBlackListDiff(getLanguageForSettingKey(language), diff);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myConfigurable;
  }

  private void createUIComponents() {
    myOptions = new HashMap<>();
    myEditors = new HashMap<>();
    myIsValidPatterns = new HashMap<>();

    List<Language> allLanguages = getBaseLanguagesWithProviders();
    Language lastEditedLanguage = lastEditedLanguage();

    Language selected = lastEditedLanguage != null ? lastEditedLanguage : allLanguages.get(0);

    initLanguageCombo(selected, allLanguages);

    myCardLayout = new CardLayout();
    myPanel = new JPanel(myCardLayout);

    allLanguages.forEach((language -> {
      JPanel panel = createLanguagePanel(language);
      panel.setName(language.getID());
      myPanel.add(panel, language.getID());
    }));

    myCardLayout.show(myPanel, selected.getID());
  }

  @Nullable
  private static Language lastEditedLanguage() {
    String id = PropertiesComponent.getInstance().getValue(LAST_EDITED_LANGUAGE_ID_KEY);
    if (id == null) return null;
    return Language.findLanguageByID(id);
  }

  private static void saveLastEditedLanguage(@NotNull String id) {
    PropertiesComponent.getInstance().setValue(LAST_EDITED_LANGUAGE_ID_KEY, id);
  }

  @NotNull
  private JPanel createLanguagePanel(@NotNull Language language) {
    JPanel blacklistPanel = createBlacklistPanel(language);
    JPanel optionsPanel = createOptionsPanel(language);

    JPanel panel = new JPanel();
    BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
    panel.setLayout(layout);

    if (blacklistPanel != null) {
      panel.add(blacklistPanel);
    }

    if (optionsPanel != null) {
      panel.add(optionsPanel);
    }

    return panel;
  }

  @Nullable
  private JPanel createBlacklistPanel(@NotNull Language language) {
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (!provider.isBlackListSupported()) return null;

    String blackList = getLanguageBlackList(language);

    EditorTextField editorTextField = createBlacklistEditorField(blackList);
    editorTextField.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent e) {
        updateOkEnabled(language, editorTextField);
      }
    });
    updateOkEnabled(language, editorTextField);

    myEditors.put(language, editorTextField);

    JPanel blacklistPanel = new JPanel();

    BoxLayout layout = new BoxLayout(blacklistPanel, BoxLayout.Y_AXIS);
    blacklistPanel.setLayout(layout);
    blacklistPanel.setBorder(IdeBorderFactory.createTitledBorder("Blacklist"));

    JBLabel explanation = new JBLabel(getBlacklistExplanationHTML(language));
    explanation.setAlignmentX(Component.LEFT_ALIGNMENT);
    blacklistPanel.add(explanation);

    JComponent resetPanel = createResetPanel(language);
    resetPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    blacklistPanel.add(resetPanel);

    editorTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
    blacklistPanel.add(editorTextField);

    JBLabel label = blacklistDependencyInfoLabel(language);
    if (label != null) {
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      blacklistPanel.add(label);
    }

    return blacklistPanel;
  }

  @Nullable
  private static JBLabel blacklistDependencyInfoLabel(@NotNull Language language) {
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    Language dependencyLanguage = provider.getBlackListDependencyLanguage();
    if (dependencyLanguage == null) {
      return null;
    }
    return new JBLabel("<html>Additionally <b>" + dependencyLanguage.getDisplayName() + "</b> language blacklist will be applied.</html>");
  }

  @NotNull
  private JComponent createResetPanel(@NotNull Language language) {
    SwingActionLink link = new SwingActionLink(new AbstractAction("Reset") {
      @Override
      public void actionPerformed(ActionEvent e) {
        setLanguageBlacklistToDefault(language);
      }
    });

    Box box = Box.createHorizontalBox();
    box.add(Box.createHorizontalGlue());
    box.add(link);

    return box;
  }

  private void setLanguageBlacklistToDefault(Language language) {
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    Set<String> defaultBlacklist = provider.getDefaultBlackList();
    EditorTextField editor = myEditors.get(language);
    editor.setText(StringUtil.join(defaultBlacklist, "\n"));
  }

  @NotNull
  private static String getBlacklistExplanationHTML(Language language) {
    InlayParameterHintsProvider hintsProvider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (hintsProvider == null) {
      return CodeInsightBundle.message("inlay.hints.blacklist.pattern.explanation");
    }
    return hintsProvider.getBlacklistExplanationHTML();
  }

  @Nullable
  private JPanel createOptionsPanel(@NotNull Language language) {
    List<Option> options = getOptions(language);
    if (options.isEmpty()) {
      return null;
    }

    JPanel languageOptionsPanel = new JPanel();
    BoxLayout boxLayout = new BoxLayout(languageOptionsPanel, BoxLayout.Y_AXIS);
    languageOptionsPanel.setLayout(boxLayout);

    if (!options.isEmpty()) {
      languageOptionsPanel.setBorder(IdeBorderFactory.createTitledBorder("Options"));
    }

    for (Option option : options) {
      JBCheckBox box = new JBCheckBox(option.getName(), option.get());
      myOptions.put(option, box);
      languageOptionsPanel.add(box);
    }

    return languageOptionsPanel;
  }

  private void initLanguageCombo(Language selected, List<Language> languages) {
    myCurrentLanguageCombo = new ComboBox<>(new CollectionComboBoxModel<>(languages));
    myCurrentLanguageCombo.setSelectedItem(selected);
    myCurrentLanguageCombo.setRenderer(SimpleListCellRenderer.create("", Language::getDisplayName));

    myCurrentLanguageCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        Language language = (Language)e.getItem();
        if (e.getStateChange() == ItemEvent.SELECTED) {
          showLanguagePanel(language);
        }
      }
    });
  }

  private void showLanguagePanel(@NotNull Language language) {
    myCardLayout.show(myPanel, language.getID());
  }

  private static List<Option> getOptions(Language language) {
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (provider != null) {
      return provider.getSupportedOptions();
    }
    return ContainerUtil.emptyList();
  }

  @NotNull
  private static String getLanguageBlackList(@NotNull Language language) {
    InlayParameterHintsProvider hintsProvider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (hintsProvider == null) {
      return "";
    }
    Diff diff = ParameterNameHintsSettings.getInstance().getBlackListDiff(getLanguageForSettingKey(language));
    Set<String> blackList = diff.applyOn(hintsProvider.getDefaultBlackList());
    return StringUtil.join(blackList, "\n");
  }

  @NotNull
  private static EditorTextField createBlacklistEditorField(@NotNull String text) {
    Document document = EditorFactory.getInstance().createDocument(text);
    EditorTextField field = new EditorTextField(document, null, FileTypes.PLAIN_TEXT, false, false);
    field.setPreferredSize(new Dimension(200, 350));
    field.addSettingsProvider(editor -> {
      editor.setVerticalScrollbarVisible(true);
      editor.setHorizontalScrollbarVisible(true);
      editor.getSettings().setAdditionalLinesCount(2);
      highlightErrorLines(getBlackListInvalidLineNumbers(text), editor);
    });
    return field;
  }
}
