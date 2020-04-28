// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.formatting.FormattingProgressTask;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.FutureTask;

public class ReformatCodeProcessor extends AbstractLayoutCodeProcessor {
  private static final Logger LOG = Logger.getInstance(ReformatCodeProcessor.class);

  private final Collection<TextRange> myRanges = new ArrayList<>();
  private SelectionModel mySelectionModel;

  public ReformatCodeProcessor(Project project, boolean processChangedTextOnly) {
    super(project, getCommandName(), getProgressText(), processChangedTextOnly);
  }

  public ReformatCodeProcessor(@NotNull PsiFile file, @NotNull SelectionModel selectionModel) {
    super(file.getProject(), file, getProgressText(), getCommandName(), false);
    mySelectionModel = selectionModel;
  }

  public ReformatCodeProcessor(AbstractLayoutCodeProcessor processor, @NotNull SelectionModel selectionModel) {
    super(processor, getCommandName(), getProgressText());
    mySelectionModel = selectionModel;
  }

  public ReformatCodeProcessor(AbstractLayoutCodeProcessor processor, boolean processChangedTextOnly) {
    super(processor, getCommandName(), getProgressText());
    setProcessChangedTextOnly(processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, Module module, boolean processChangedTextOnly) {
    super(project, module, getCommandName(), getProgressText(), processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiDirectory directory, boolean includeSubdirs, boolean processChangedTextOnly) {
    super(project, directory, includeSubdirs, getProgressText(), getCommandName(), processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiFile file, @Nullable TextRange range, boolean processChangedTextOnly) {
    super(project, file, getProgressText(), getCommandName(), processChangedTextOnly);
    if (range != null) {
      myRanges.add(range);
    }
  }

  public ReformatCodeProcessor(@NotNull PsiFile file, boolean processChangedTextOnly) {
    super(file.getProject(), file, getProgressText(), getCommandName(), processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiFile[] files, @Nullable Runnable postRunnable, boolean processChangedTextOnly) {
    this(project, files, getCommandName(), postRunnable, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project,
                               PsiFile[] files,
                               String commandName,
                               @Nullable Runnable postRunnable,
                               boolean processChangedTextOnly)
  {
    super(project, files, getProgressText(), commandName, postRunnable, processChangedTextOnly);
  }

  @Override
  @NotNull
  protected FutureTask<Boolean> prepareTask(@NotNull final PsiFile file, final boolean processChangedTextOnly)
    throws IncorrectOperationException
  {
    return new FutureTask<>(() -> {
      FormattingProgressTask.FORMATTING_CANCELLED_FLAG.set(false);
      try {
        PsiFile fileToProcess = ensureValid(file);
        if (fileToProcess == null) return false;

        CharSequence before = null;
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(fileToProcess);
        if (getInfoCollector() != null) {
          LOG.assertTrue(document != null);
          before = document.getImmutableCharSequence();
        }

        EditorScrollingPositionKeeper.perform(document, true, () -> {
          if (processChangedTextOnly) {
            ChangedRangesInfo info = VcsFacade.getInstance().getChangedRangesInfo(fileToProcess);
            if (info != null) {
              assertFileIsValid(fileToProcess);
              CodeStyleManager.getInstance(myProject).reformatTextWithContext(fileToProcess, info);
            }
          }
          else {
            Collection<TextRange> ranges = getRangesToFormat(fileToProcess);
            CodeStyleManager.getInstance(myProject).reformatText(fileToProcess, ranges);
          }
        });

        if (before != null) {
          prepareUserNotificationMessage(document, before);
        }

        return !FormattingProgressTask.FORMATTING_CANCELLED_FLAG.get();
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
        return false;
      }
      finally {
        myRanges.clear();
      }
    });
  }

  @Nullable
  private static PsiFile ensureValid(@NotNull PsiFile file) {
    if (file.isValid()) return file;

    VirtualFile virtualFile = file.getVirtualFile();
    if (!virtualFile.isValid()) return null;

    FileViewProvider provider = file.getManager().findViewProvider(virtualFile);
    if (provider == null) return null;

    Language language = file.getLanguage();
    return provider.hasLanguage(language) ? provider.getPsi(language) : provider.getPsi(provider.getBaseLanguage());
  }

  private static void assertFileIsValid(@NotNull PsiFile file) {
    if (!file.isValid()) {
      LOG.error(
        "Invalid Psi file, name: " + file.getName() +
        " , class: " + file.getClass().getSimpleName() +
        " , " + PsiInvalidElementAccessException.findOutInvalidationReason(file));
    }
  }

  private void prepareUserNotificationMessage(@NotNull Document document, @NotNull CharSequence before) {
    LOG.assertTrue(getInfoCollector() != null);
    int number = VcsFacade.getInstance().calculateChangedLinesNumber(document, before);
    if (number > 0) {
      String message = "formatted " + number + " line" + (number > 1 ? "s" : "");
      getInfoCollector().setReformatCodeNotification(message);
    }
  }

  @NotNull
  private Collection<TextRange> getRangesToFormat(PsiFile file) {
    if (mySelectionModel != null) {
      return getSelectedRanges(mySelectionModel);
    }
    
    return !myRanges.isEmpty() ? myRanges : ContainerUtil.newArrayList(file.getTextRange());
  }

  private static String getProgressText() {
    return CodeInsightBundle.message("reformat.progress.common.text");
  }

  public static String getCommandName() {
    return CodeInsightBundle.message("process.reformat.code");
  }
}