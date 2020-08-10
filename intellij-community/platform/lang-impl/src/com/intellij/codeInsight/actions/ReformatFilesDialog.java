// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.stream.Stream;

import static com.intellij.codeInsight.actions.TextRangeType.VCS_CHANGED_TEXT;
import static com.intellij.codeInsight.actions.TextRangeType.WHOLE_FILE;

public class ReformatFilesDialog extends DialogWrapper implements ReformatFilesOptions {
  private JPanel myPanel;
  private JCheckBox myOptimizeImports;
  private JCheckBox myOnlyChangedText;
  private JCheckBox myRearrangeEntriesCb;
  private JCheckBox myCleanupCode;

  private final LastRunReformatCodeOptionsProvider myLastRunSettings;

  public ReformatFilesDialog(@NotNull Project project, VirtualFile @NotNull [] files) {
    super(project, true);
    myLastRunSettings = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());

    boolean canTargetVcsChanges = VcsFacade.getInstance().hasChanges(files, project);
    myOnlyChangedText.setEnabled(canTargetVcsChanges);
    myOnlyChangedText.setSelected(canTargetVcsChanges && myLastRunSettings.getLastTextRangeType() == VCS_CHANGED_TEXT);

    boolean canOptimizeImports = areImportOptimizersAvailable(files, project);
    myOptimizeImports.setEnabled(canOptimizeImports);
    myOptimizeImports.setSelected(canOptimizeImports && myLastRunSettings.getLastOptimizeImports());

    myCleanupCode.setSelected(myLastRunSettings.getLastCodeCleanup());
    myRearrangeEntriesCb.setSelected(myLastRunSettings.getLastRearrangeCode());
    myRearrangeEntriesCb.setEnabled(containsAtLeastOneFileToRearrange(files));

    setTitle(CodeInsightBundle.message("dialog.reformat.files.title"));
    init();
  }

  private static boolean areImportOptimizersAvailable(VirtualFile @NotNull [] files, @NotNull Project project) {
    PsiManager psiManager = PsiManager.getInstance(project);
    return Stream.of(files)
      .map(psiManager::findFile)
      .filter(Objects::nonNull)
      .flatMap(file -> OptimizeImportsProcessor.collectOptimizers(file).stream())
      .findFirst().isPresent();
  }

  private static boolean containsAtLeastOneFileToRearrange(VirtualFile @NotNull [] files) {
    for (VirtualFile file : files) {
      FileType fileType = file.getFileType();
      if (fileType instanceof LanguageFileType) {
        Language language = ((LanguageFileType)fileType).getLanguage();
        if (Rearranger.EXTENSION.forLanguage(language) != null) {
          return true;
        }
      }

    }
    return false;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  public boolean isOptimizeImports() {
    return myOptimizeImports.isSelected();
  }

  @Override
  public TextRangeType getTextRangeType() {
    return myOnlyChangedText.isEnabled() && myOnlyChangedText.isSelected()
           ? VCS_CHANGED_TEXT
           : WHOLE_FILE;
  }

  @Override
  public boolean isRearrangeCode() {
    return myRearrangeEntriesCb.isSelected();
  }

  @Override
  public boolean isCodeCleanup() {
    return myCleanupCode.isSelected();
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();
    if (myOptimizeImports.isEnabled()) {
      myLastRunSettings.saveOptimizeImportsState(isOptimizeImports());
    }

    myLastRunSettings.saveCodeCleanupState(isCodeCleanup());
    if (myRearrangeEntriesCb.isEnabled()) {
      myLastRunSettings.saveRearrangeCodeState(isRearrangeCode());
    }
    if (myOnlyChangedText.isEnabled()) {
      myLastRunSettings.saveProcessVcsChangedTextState(getTextRangeType() == VCS_CHANGED_TEXT);
    }
  }

  @Nullable
  @Override
  public SearchScope getSearchScope() {
    return null;
  }

  @Nullable
  @Override
  public String getFileTypeMask() {
    return null;
  }
}
