/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.codeInsight.actions.TextRangeType.VCS_CHANGED_TEXT;
import static com.intellij.codeInsight.actions.TextRangeType.WHOLE_FILE;

public class ReformatFilesDialog extends DialogWrapper implements ReformatFilesOptions {
  private JPanel myPanel;
  private JCheckBox myOptimizeImports;
  private JCheckBox myOnlyChangedText;
  private JCheckBox myRearrangeEntriesCb;
  private JCheckBox myCleanupCode;

  private final LastRunReformatCodeOptionsProvider myLastRunSettings;

  public ReformatFilesDialog(@NotNull Project project, @NotNull VirtualFile[] files) {
    super(project, true);
    myLastRunSettings = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());

    boolean canTargetVcsChanges = FormatChangedTextUtil.hasChanges(files, project);
    myOnlyChangedText.setEnabled(canTargetVcsChanges);
    myOnlyChangedText.setSelected(canTargetVcsChanges && myLastRunSettings.getLastTextRangeType() == VCS_CHANGED_TEXT);
    myOptimizeImports.setSelected(myLastRunSettings.getLastOptimizeImports());
    myCleanupCode.setSelected(myLastRunSettings.getLastCodeCleanup());
    myRearrangeEntriesCb.setSelected(myLastRunSettings.getLastRearrangeCode());
    myRearrangeEntriesCb.setEnabled(containsAtLeastOneFileToRearrange(files));

    setTitle(CodeInsightBundle.message("dialog.reformat.files.title"));
    init();
  }
  
  private static boolean containsAtLeastOneFileToRearrange(@NotNull VirtualFile[] files) {
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
  public boolean isOptimizeImports(){
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
    myLastRunSettings.saveOptimizeImportsState(isOptimizeImports());
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
