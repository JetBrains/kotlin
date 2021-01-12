/*
 * Copyright 2010 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeEditorHighlighterProviders;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Base class for code style settings panels supporting multiple programming languages.
 *
 * @author rvishnyakov
 */
public abstract class CustomizableLanguageCodeStylePanel extends CodeStyleAbstractPanel implements CodeStyleSettingsCustomizable {
  private static final Logger LOG = Logger.getInstance(CustomizableLanguageCodeStylePanel.class);

  protected CustomizableLanguageCodeStylePanel(CodeStyleSettings settings) {
    super(settings);
  }

  protected void init() {
    customizeSettings();
  }

  protected void customizeSettings() {
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(getDefaultLanguage());
    if (provider != null) {
      provider.customizeSettings(this, getSettingsType());
    }
  }

  public abstract LanguageCodeStyleSettingsProvider.SettingsType getSettingsType();


  protected void resetDefaultNames() {
  }

  @Override
  protected String getPreviewText() {
    if (getDefaultLanguage() == null) return "";
    String sample = LanguageCodeStyleSettingsProvider.getCodeSample(getDefaultLanguage(), getSettingsType());
    if (sample == null) return "";
    return sample;
  }

  @Override
  protected int getRightMargin() {
    if (getDefaultLanguage() == null) return -1;
    return LanguageCodeStyleSettingsProvider.getRightMargin(getDefaultLanguage(), getSettingsType());
  }

  @Override
  protected String getFileExt() {
    String fileExt = LanguageCodeStyleSettingsProvider.getFileExt(getDefaultLanguage());
    if (fileExt != null) return fileExt;
    return super.getFileExt();
  }

  @NotNull
  @Override
  protected FileType getFileType() {
    if (getDefaultLanguage() != null) {
      FileType assocType = getDefaultLanguage().getAssociatedFileType();
      if (assocType != null) {
        return assocType;
      }
    }
    return StdFileTypes.JAVA;
  }

  @Override
  @Nullable
  protected EditorHighlighter createHighlighter(final EditorColorsScheme scheme) {
    FileType fileType = getFileType();
    return FileTypeEditorHighlighterProviders.INSTANCE.forFileType(fileType).getEditorHighlighter(
      ProjectUtil.guessCurrentProject(getPanel()), fileType, null, scheme);
  }


  @Override
  protected PsiFile doReformat(final Project project, final PsiFile psiFile) {
    final String text = psiFile.getText();
    final PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
    final Document doc = manager.getDocument(psiFile);
    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
      if (doc != null) {
        doc.replaceString(0, doc.getTextLength(), text);
        manager.commitDocument(doc);
      }
      try {
        super.doReformat(project, psiFile);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }), "", "");
    if (doc != null) {
      manager.commitDocument(doc);
    }
    return psiFile;
  }

  protected static JPanel createPreviewPanel() {
    return new JPanel(new BorderLayout());
  }

  @Override
  public void moveStandardOption(String fieldName, String newGroup) {
    throw new UnsupportedOperationException();
  }

  protected <T extends OrderedOption>List<T> sortOptions(Collection<? extends T> options) {
    Set<String> names = new THashSet<>(ContainerUtil.map(options, (Function<OrderedOption, String>)option -> option.getOptionName()));

    List<T> order = new ArrayList<>(options.size());
    MultiMap<String, T> afters = new MultiMap<>();
    MultiMap<String, T> befores = new MultiMap<>();

    for (T each : options) {
        String anchorOptionName = each.getAnchorOptionName();
        if (anchorOptionName != null && names.contains(anchorOptionName)) {
          if (each.getAnchor() == OptionAnchor.AFTER) {
            afters.putValue(anchorOptionName, each);
            continue;
          }
          else if (each.getAnchor() == OptionAnchor.BEFORE) {
            befores.putValue(anchorOptionName, each);
            continue;
          }
        }
      order.add(each);
    }

    List<T> result = new ArrayList<>(options.size());
    for (T each : order) {
      result.addAll(befores.get(each.getOptionName()));
      result.add(each);
      result.addAll(afters.get(each.getOptionName()));
    }

    assert result.size() == options.size();
    return result;
  }

  protected abstract static class OrderedOption {
    @NotNull private final String optionName;
    @Nullable private final OptionAnchor anchor;
    @Nullable private final String anchorOptionName;

    protected OrderedOption(@NotNull String optionName,
                            @Nullable OptionAnchor anchor,
                            @Nullable String anchorOptionName) {
      this.optionName = optionName;
      this.anchor = anchor;
      this.anchorOptionName = anchorOptionName;
    }

    @NotNull
    public String getOptionName() {
      return optionName;
    }

    @Nullable
    public OptionAnchor getAnchor() {
      return anchor;
    }

    @Nullable
    public String getAnchorOptionName() {
      return anchorOptionName;
    }
  }
}
