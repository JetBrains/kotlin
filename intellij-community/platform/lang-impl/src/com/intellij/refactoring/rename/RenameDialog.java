// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename;

import com.intellij.find.FindBundle;
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo;
import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.ui.SeparatorFactory;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import com.intellij.xml.util.XmlTagUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class RenameDialog extends RefactoringDialog {
  private static final String REFACTORING_NAME = RefactoringBundle.message("rename.title");

  private SuggestedNameInfo mySuggestedNameInfo;
  private JLabel myNameLabel;
  private NameSuggestionsField myNameSuggestionsField;
  private JCheckBox myCbSearchInComments;
  private JCheckBox myCbSearchTextOccurrences;
  private final JLabel myNewNamePrefix = new JLabel("");
  private final String myHelpID;
  private final PsiElement myPsiElement;
  private final PsiElement myNameSuggestionContext;
  private final Editor myEditor;
  private NameSuggestionsField.DataChanged myNameChangedListener;
  private final Map<AutomaticRenamerFactory, JCheckBox> myAutoRenamerFactories = new HashMap<>();
  private String myOldName;

  private ScopeChooserCombo myScopeCombo;

  public RenameDialog(@NotNull Project project, @NotNull PsiElement psiElement, @Nullable PsiElement nameSuggestionContext, Editor editor) {
    super(project, true);

    PsiUtilCore.ensureValid(psiElement);

    myPsiElement = psiElement;
    myNameSuggestionContext = nameSuggestionContext;
    myEditor = editor;
    setTitle(REFACTORING_NAME);

    createNewNameComponent();
    init();

    myNameLabel.setText(XmlStringUtil.wrapInHtml(XmlTagUtilBase.escapeString(getLabelText(), false)));
    boolean toSearchInComments = isToSearchInCommentsForRename();
    myCbSearchInComments.setSelected(toSearchInComments);

    if (myCbSearchTextOccurrences.isEnabled()) {
      boolean toSearchForTextOccurrences = isToSearchForTextOccurrencesForRename();
      myCbSearchTextOccurrences.setSelected(toSearchForTextOccurrences);
    }

    if (!ApplicationManager.getApplication().isUnitTestMode()) validateButtons();
    myHelpID = RenamePsiElementProcessor.forElement(psiElement).getHelpID(psiElement);
  }

  public static void showRenameDialog(DataContext dataContext, RenameDialog dialog) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final String name = PsiElementRenameHandler.DEFAULT_NAME.getData(dataContext);
      dialog.performRename(name);
      dialog.close(OK_EXIT_CODE);
    }
    else {
      dialog.show();
    }
  }

  @NotNull
  protected String getLabelText() {
    return RefactoringBundle.message("rename.0.and.its.usages.to", getFullName());
  }

  @NotNull
  public PsiElement getPsiElement() {
    return myPsiElement;
  }

  @Override
  protected boolean hasPreviewButton() {
    return RenamePsiElementProcessor.forElement(myPsiElement).showRenamePreviewButton(myPsiElement);
  }

  @Override
  protected void dispose() {
    myNameSuggestionsField.removeDataChangedListener(myNameChangedListener);
    super.dispose();
  }

  protected boolean isToSearchForTextOccurrencesForRename() {
    return RenamePsiElementProcessor.forElement(myPsiElement).isToSearchForTextOccurrences(myPsiElement);
  }

  protected boolean isToSearchInCommentsForRename() {
    return RenamePsiElementProcessor.forElement(myPsiElement).isToSearchInComments(myPsiElement);
  }

  protected String getFullName() {
    String name = DescriptiveNameUtil.getDescriptiveName(myPsiElement);
    String type = UsageViewUtil.getType(myPsiElement);
    return StringUtil.isEmpty(name) ? type : type + " '" + name + "'";
  }

  protected void createNewNameComponent() {
    String[] suggestedNames = getSuggestedNames();
    myOldName = UsageViewUtil.getShortName(myPsiElement);
    myNameSuggestionsField = new NameSuggestionsField(suggestedNames, myProject, FileTypes.PLAIN_TEXT, myEditor) {
      @Override
      protected boolean shouldSelectAll() {
        return myEditor == null || myEditor.getSettings().isPreselectRename();
      }
    };
    if (myPsiElement instanceof PsiFile && myEditor == null) {
      myNameSuggestionsField.selectNameWithoutExtension();
    }
    myNameChangedListener = () -> processNewNameChanged();
    myNameSuggestionsField.addDataChangedListener(myNameChangedListener);
  }

  protected void preselectExtension(int start, int end) {
    myNameSuggestionsField.select(start, end);
  }

  protected void processNewNameChanged() {
    validateButtons();
  }

  public String[] getSuggestedNames() {
    final LinkedHashSet<String> result = new LinkedHashSet<>();
    final String initialName = VariableInplaceRenameHandler.getInitialName();
    if (initialName != null) {
      result.add(initialName);
    }
    result.add(UsageViewUtil.getShortName(myPsiElement));
    mySuggestedNameInfo = NameSuggestionProvider.suggestNames(myPsiElement, myNameSuggestionContext, result);
    return ArrayUtilRt.toStringArray(result);
  }

  @NotNull
  public String getNewName() {
    return myNameSuggestionsField.getEnteredName().trim();
  }

  @NotNull
  public SearchScope getRefactoringScope() {
    SearchScope scope = myScopeCombo.getSelectedScope();
    return scope != null ? scope : GlobalSearchScope.projectScope(myProject);
  }

  public boolean isSearchInComments() {
    return myCbSearchInComments.isSelected();
  }

  public boolean isSearchInNonJavaFiles() {
    return myCbSearchTextOccurrences.isSelected();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameSuggestionsField.getFocusableComponent();
  }

  @Override
  protected JComponent createCenterPanel() {
    return null;
  }

  @Override
  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();

    gbConstraints.insets = JBUI.insetsBottom(4);
    gbConstraints.weighty = 0;
    gbConstraints.weightx = 1;
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.fill = GridBagConstraints.BOTH;
    myNameLabel = new JLabel();
    panel.add(myNameLabel, gbConstraints);

    gbConstraints.insets = JBUI.insets(0, 0, 4, StringUtil.isEmpty(myNewNamePrefix.getText()) ? 0 : 1);
    gbConstraints.gridwidth = 1;
    gbConstraints.fill = GridBagConstraints.NONE;
    gbConstraints.weightx = 0;
    gbConstraints.gridx = 0;
    gbConstraints.anchor = GridBagConstraints.WEST;
    panel.add(myNewNamePrefix, gbConstraints);

    gbConstraints.insets = JBUI.insetsBottom(8);
    gbConstraints.gridwidth = 2;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.weightx = 1;
    gbConstraints.gridx = 0;
    gbConstraints.weighty = 1;
    panel.add(myNameSuggestionsField.getComponent(), gbConstraints);

    createCheckboxes(panel, gbConstraints);
    JComponent scopePanel = createSearchScopePanel();
    if (scopePanel != null) {
      gbConstraints.insets = JBUI.insetsBottom(8);
      gbConstraints.gridx = 0;
      gbConstraints.gridy = GridBagConstraints.RELATIVE;
      gbConstraints.gridwidth = 2;
      gbConstraints.fill = GridBagConstraints.BOTH;
      panel.add(scopePanel, gbConstraints);
    }
    return panel;
  }

  protected void createCheckboxes(JPanel panel, GridBagConstraints gbConstraints) {
    gbConstraints.insets = JBUI.insetsBottom(4);
    gbConstraints.gridwidth = 1;
    gbConstraints.gridx = 0;
    gbConstraints.weighty = 0;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    myCbSearchInComments = new NonFocusableCheckBox();
    myCbSearchInComments.setText(RefactoringBundle.getSearchInCommentsAndStringsText());
    myCbSearchInComments.setSelected(true);
    panel.add(myCbSearchInComments, gbConstraints);

    gbConstraints.insets = JBUI.insets(0, UIUtil.DEFAULT_HGAP, 4, 0);
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.gridx = 1;
    gbConstraints.weightx = 1;
    gbConstraints.fill = GridBagConstraints.BOTH;
    myCbSearchTextOccurrences = new NonFocusableCheckBox();
    myCbSearchTextOccurrences.setText(RefactoringBundle.getSearchForTextOccurrencesText());
    myCbSearchTextOccurrences.setSelected(true);
    panel.add(myCbSearchTextOccurrences, gbConstraints);
    if (!TextOccurrencesUtil.isSearchTextOccurrencesEnabled(myPsiElement)) {
      myCbSearchTextOccurrences.setEnabled(false);
      myCbSearchTextOccurrences.setSelected(false);
      myCbSearchTextOccurrences.setVisible(false);
    }

    for(AutomaticRenamerFactory factory: AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
      if (factory.isApplicable(myPsiElement) && factory.getOptionName() != null) {
        gbConstraints.gridwidth = myAutoRenamerFactories.size() % 2 == 0 ? 1 : GridBagConstraints.REMAINDER;
        gbConstraints.gridx = myAutoRenamerFactories.size() % 2;
        gbConstraints.insets = gbConstraints.gridx == 0 ? JBUI.insetsBottom(4) : JBUI.insets(0, UIUtil.DEFAULT_HGAP, 4, 0);
        gbConstraints.weightx = 1;
        gbConstraints.fill = GridBagConstraints.BOTH;

        JCheckBox checkBox = new NonFocusableCheckBox();
        checkBox.setText(factory.getOptionName());
        checkBox.setSelected(factory.isEnabled());
        panel.add(checkBox, gbConstraints);
        myAutoRenamerFactories.put(factory, checkBox);
      }
    }
  }

  @Nullable
  protected JComponent createSearchScopePanel() {
    myScopeCombo = new ScopeChooserCombo(myProject, false, true, "Project Files");
    Disposer.register(myDisposable, myScopeCombo);

    // do not show scope chooser for local variables
    SearchScope useScope = PsiSearchHelper.getInstance(myProject).getUseScope(myPsiElement);
    if (useScope instanceof LocalSearchScope) return null;

    JPanel optionsPanel = new JPanel(new BorderLayout());
    optionsPanel.add(myScopeCombo, BorderLayout.CENTER);
    JComponent separator = SeparatorFactory.createSeparator(FindBundle.message("find.scope.label"), myScopeCombo.getComboBox());
    optionsPanel.add(separator, BorderLayout.NORTH);
    return optionsPanel;
  }

  @Override
  protected String getHelpId() {
    return myHelpID;
  }

  @Override
  protected void doAction() {
    PsiUtilCore.ensureValid(myPsiElement);
    String newName = getNewName();
    performRename(newName);
  }

  public void performRename(@NotNull String newName) {
    final RenamePsiElementProcessor elementProcessor = RenamePsiElementProcessor.forElement(myPsiElement);
    elementProcessor.setToSearchInComments(myPsiElement, isSearchInComments());
    if (myCbSearchTextOccurrences.isEnabled()) {
      elementProcessor.setToSearchForTextOccurrences(myPsiElement, isSearchInNonJavaFiles());
    }
    if (mySuggestedNameInfo != null) {
      mySuggestedNameInfo.nameChosen(newName);
    }

    final RenameProcessor processor = createRenameProcessor(newName);

    for(Map.Entry<AutomaticRenamerFactory, JCheckBox> e: myAutoRenamerFactories.entrySet()) {
      e.getKey().setEnabled(e.getValue().isSelected());
      if (e.getValue().isSelected()) {
        processor.addRenamerFactory(e.getKey());
      }
    }

    invokeRefactoring(processor);
  }

  protected RenameProcessor createRenameProcessor(@NotNull String newName) {
    return new RenameProcessor(getProject(), myPsiElement, newName, getRefactoringScope(), isSearchInComments(), isSearchInNonJavaFiles());
  }

  @Override
  protected void canRun() throws ConfigurationException {
    if (Comparing.strEqual(getNewName(), myOldName)) throw new ConfigurationException(null);
    if (!areButtonsValid()) {
      throw new ConfigurationException("\'" + getNewName() + "\' is not a valid identifier");
    }
    final Function<String, String> inputValidator = RenameInputValidatorRegistry.getInputErrorValidator(myPsiElement);
    if (inputValidator != null) {
      setErrorText(inputValidator.fun(getNewName()));
    }
  }

  @Override
  protected boolean areButtonsValid() {
    final String newName = getNewName();
    return RenameUtil.isValidName(myProject, myPsiElement, newName);
  }

  protected NameSuggestionsField getNameSuggestionsField() {
    return myNameSuggestionsField;
  }

  public JCheckBox getCbSearchInComments() {
    return myCbSearchInComments;
  }
}