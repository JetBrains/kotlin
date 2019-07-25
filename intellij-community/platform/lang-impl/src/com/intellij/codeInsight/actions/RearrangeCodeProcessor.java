/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import com.intellij.psi.codeStyle.arrangement.engine.ArrangementEngine;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.diff.FilesTooBigForDiffException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.FutureTask;

public class RearrangeCodeProcessor extends AbstractLayoutCodeProcessor {

  public static final String COMMAND_NAME = "Rearrange code";
  public static final String PROGRESS_TEXT = CodeInsightBundle.message("process.rearrange.code");

  private static final Logger LOG = Logger.getInstance(RearrangeCodeProcessor.class);
  private SelectionModel mySelectionModel;

  public RearrangeCodeProcessor(@NotNull AbstractLayoutCodeProcessor previousProcessor) {
    super(previousProcessor, COMMAND_NAME, PROGRESS_TEXT);
  }

  public RearrangeCodeProcessor(@NotNull AbstractLayoutCodeProcessor previousProcessor, @NotNull SelectionModel selectionModel) {
    super(previousProcessor, COMMAND_NAME, PROGRESS_TEXT);
    mySelectionModel = selectionModel;
  }

  public RearrangeCodeProcessor(@NotNull PsiFile file, @NotNull SelectionModel selectionModel) {
    super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, false);
    mySelectionModel = selectionModel;
  }

  public RearrangeCodeProcessor(@NotNull PsiFile file) {
    super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, false);
  }

  @SuppressWarnings("unused") // Required for compatibility with external plugins.
  public RearrangeCodeProcessor(@NotNull Project project,
                                @NotNull PsiFile[] files,
                                @NotNull String commandName,
                                @Nullable Runnable postRunnable) {
    this(project, files, commandName, postRunnable, false);
  }

  public RearrangeCodeProcessor(@NotNull Project project,
                                @NotNull PsiFile[] files,
                                @NotNull String commandName,
                                @Nullable Runnable postRunnable,
                                boolean processChangedTextOnly) {
    super(project, files, PROGRESS_TEXT, commandName, postRunnable, processChangedTextOnly);
  }

  @NotNull
  @Override
  protected FutureTask<Boolean> prepareTask(@NotNull final PsiFile file, final boolean processChangedTextOnly) {
    return new FutureTask<>(() -> {
      try {
        Collection<TextRange> ranges = getRangesToFormat(file, processChangedTextOnly);
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);

        if (document != null && Rearranger.EXTENSION.forLanguage(file.getLanguage()) != null) {
          PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(document);
          PsiDocumentManager.getInstance(myProject).commitDocument(document);
          Runnable command = prepareRearrangeCommand(file, ranges);
          try {
            CommandProcessor.getInstance().executeCommand(myProject, command, COMMAND_NAME, null);
          }
          finally {
            PsiDocumentManager.getInstance(myProject).commitDocument(document);
          }
        }

        return true;
      }
      catch (FilesTooBigForDiffException e) {
        handleFileTooBigException(LOG, e, file);
        return false;
      }
    });
  }

  @NotNull
  private Runnable prepareRearrangeCommand(@NotNull final PsiFile file, @NotNull final Collection<TextRange> ranges) {
    final ArrangementEngine engine = ServiceManager.getService(myProject, ArrangementEngine.class);
    return () -> {
      engine.arrange(file, ranges);
      if (getInfoCollector() != null) {
        String info = engine.getUserNotificationInfo();
        getInfoCollector().setRearrangeCodeNotification(info);
      }
    };
  }

  public Collection<TextRange> getRangesToFormat(@NotNull PsiFile file, boolean processChangedTextOnly) throws FilesTooBigForDiffException {
    if (mySelectionModel != null) {
      return getSelectedRanges(mySelectionModel);
    }

    if (processChangedTextOnly) {
      return FormatChangedTextUtil.getInstance().getChangedTextRanges(myProject, file);
    }

    return ContainerUtil.newSmartList(file.getTextRange());
  }
}
