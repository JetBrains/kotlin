/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.ide.scratch;

import com.intellij.ide.IdeView;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author gregsh
 */
public abstract class ScratchFileCreationHelper {
  public static final LanguageExtension<ScratchFileCreationHelper> EXTENSION = new LanguageExtension<>(
    "com.intellij.scratch.creationHelper", new ScratchFileCreationHelper() {
  });

  /**
   * Override to change the default initial text for a scratch file stored in {@link Context#text} field.
   * Return true if the text is set up as needed and no further considerations are necessary.
   */
  public boolean prepareText(@NotNull Project project, @NotNull Context context, @NotNull DataContext dataContext) {
    return false;
  }
  
  public void beforeCreate(@NotNull Project project, @NotNull Context context) {
  } 
  
  public static class Context {
    @NotNull
    public String text = "";
    public Language language;
    public int caretOffset;
    
    public String filePrefix;
    public Factory<Integer> fileCounter;
    public String fileExtension;
    
    public ScratchFileService.Option createOption = ScratchFileService.Option.create_new_always;
    public IdeView ideView;
  }

  @Nullable
  public static PsiFile parseHeader(@NotNull Project project,
                                    @NotNull Language language,
                                    @NotNull String text) {
    LanguageFileType fileType = language.getAssociatedFileType();
    CharSequence fileSnippet = StringUtil.first(text, 10 * 1024, false);
    PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
    return fileFactory.createFileFromText(
      PathUtil.makeFileName("a", fileType == null ? "" : fileType.getDefaultExtension()),
      language, fileSnippet);
  }

  @NotNull
  public static String reformat(@NotNull Project project,
                                @NotNull Language language,
                                @NotNull String text) {
    return WriteCommandAction.runWriteCommandAction(project, (Computable<String>)() -> {
      PsiFile psi = parseHeader(project, language, text);
      if (psi != null) CodeStyleManager.getInstance(project).reformat(psi);
      return psi == null ? text : psi.getText();
    });
  }
}
