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
  
  public static final String COMMAND_NAME = CodeInsightBundle.message("process.reformat.code");

  private static final Logger LOG = Logger.getInstance(ReformatCodeProcessor.class);

  private static final String PROGRESS_TEXT = CodeInsightBundle.message("reformat.progress.common.text");
  private final Collection<TextRange> myRanges = new ArrayList<>();
  private SelectionModel mySelectionModel;

  public ReformatCodeProcessor(Project project, boolean processChangedTextOnly) {
    super(project, COMMAND_NAME, PROGRESS_TEXT, processChangedTextOnly);
  }

  public ReformatCodeProcessor(@NotNull PsiFile file, @NotNull SelectionModel selectionModel) {
    super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, false);
    mySelectionModel = selectionModel;
  }

  public ReformatCodeProcessor(AbstractLayoutCodeProcessor processor, @NotNull SelectionModel selectionModel) {
    super(processor, COMMAND_NAME, PROGRESS_TEXT);
    mySelectionModel = selectionModel;
  }

  public ReformatCodeProcessor(AbstractLayoutCodeProcessor processor, boolean processChangedTextOnly) {
    super(processor, COMMAND_NAME, PROGRESS_TEXT);
    setProcessChangedTextOnly(processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, Module module, boolean processChangedTextOnly) {
    super(project, module, COMMAND_NAME, PROGRESS_TEXT, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiDirectory directory, boolean includeSubdirs, boolean processChangedTextOnly) {
    super(project, directory, includeSubdirs, PROGRESS_TEXT, COMMAND_NAME, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiFile file, @Nullable TextRange range, boolean processChangedTextOnly) {
    super(project, file, PROGRESS_TEXT, COMMAND_NAME, processChangedTextOnly);
    if (range != null) {
      myRanges.add(range);
    }
  }

  public ReformatCodeProcessor(@NotNull PsiFile file, boolean processChangedTextOnly) {
    super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiFile[] files, @Nullable Runnable postRunnable, boolean processChangedTextOnly) {
    this(project, files, COMMAND_NAME, postRunnable, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project,
                               PsiFile[] files,
                               String commandName,
                               @Nullable Runnable postRunnable,
                               boolean processChangedTextOnly)
  {
    super(project, files, PROGRESS_TEXT, commandName, postRunnable, processChangedTextOnly);
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
            ChangedRangesInfo info = FormatChangedTextUtil.getInstance().getChangedRangesInfo(fileToProcess);
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
    int number = FormatChangedTextUtil.getInstance().calculateChangedLinesNumber(document, before);
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
}