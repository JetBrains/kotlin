// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ven
 */
public class RenameElementFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private static final Logger LOG = Logger.getInstance(RenameElementFix.class);

  private final String myNewName;
  private final String myText;

  public RenameElementFix(@NotNull PsiNamedElement element) {
    super(element);
    final VirtualFile vFile = element.getContainingFile().getVirtualFile();
    assert vFile != null : element;
    myNewName = vFile.getNameWithoutExtension();
    myText = CodeInsightBundle.message("rename.public.class.text", element.getName(), myNewName);
  }

  public RenameElementFix(@NotNull PsiNamedElement element, @NotNull String newName) {
    this(element, newName, CodeInsightBundle.message("rename.named.element.text", element.getName(), newName));
  }

  public RenameElementFix(@NotNull PsiNamedElement element, @NotNull String newName, @NotNull String text) {
    super(element);
    myNewName = newName;
    myText = text;
  }

  @Override
  @NotNull
  public String getText() {
    return myText;
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return CodeInsightBundle.message("rename.element.family");
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    if (isAvailable(project, null, file)) {
      LOG.assertTrue(file == startElement.getContainingFile());
      if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
      RenameProcessor processor = new RenameProcessor(project, startElement, myNewName, false, false);
      processor.run();
    }
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    return RenameUtil.isValidName(project, startElement, myNewName);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}