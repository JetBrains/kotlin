/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.intellij.psi.codeStyle.autodetect.IndentOptionsAdjuster;
import com.intellij.psi.codeStyle.autodetect.IndentOptionsDetectorImpl;
import com.intellij.util.Time;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ExecutorService;


class DetectAndAdjustIndentOptionsTask {
  private static final ExecutorService BOUNDED_EXECUTOR = SequentialTaskExecutor.createSequentialApplicationPoolExecutor(
    "DetectableIndentOptionsProvider Pool");
  private static final Logger LOG = Logger.getInstance(DetectAndAdjustIndentOptionsTask.class);
  private static final int INDENT_COMPUTATION_TIMEOUT = 5 * Time.SECOND;

  private final Document myDocument;
  private final Project myProject;
  private final TimeStampedIndentOptions myOptionsToAdjust;

  DetectAndAdjustIndentOptionsTask(@NotNull Project project, @NotNull Document document, @NotNull TimeStampedIndentOptions toAdjust) {
    myProject = project;
    myDocument = document;
    myOptionsToAdjust = toAdjust;
  }
  
  private PsiFile getFile() {
    return PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
  }

  @NotNull
  private Runnable calcIndentAdjuster(@NotNull ProgressIndicator indicator) {
    PsiFile file = getFile();
    IndentOptionsAdjuster adjuster = file == null ? null : new IndentOptionsDetectorImpl(file, indicator).getIndentOptionsAdjuster();
    return adjuster != null ? () -> adjustOptions(adjuster) : EmptyRunnable.INSTANCE;
  }

  private void adjustOptions(IndentOptionsAdjuster adjuster) {
    final PsiFile file = getFile();
    if (file == null) return;

    final IndentOptions currentDefault = getDefaultIndentOptions(file, myDocument);
    myOptionsToAdjust.copyFrom(currentDefault);

    adjuster.adjust(myOptionsToAdjust);
    myOptionsToAdjust.setTimeStamp(myDocument.getModificationStamp());
    myOptionsToAdjust.setOriginalIndentOptionsHash(currentDefault.hashCode());

    if (!currentDefault.equals(myOptionsToAdjust)) {
      myOptionsToAdjust.setDetected(true);
      myOptionsToAdjust.setOverrideLanguageOptions(true);
      CodeStyleSettingsManager.getInstance(myProject).fireCodeStyleSettingsChanged(file);
    }
  }

  private void logTooLongComputation() {
    PsiFile file = getFile();
    String fileName = file != null ? file.getName() : "";
    LOG.debug("Indent detection is too long for: " + fileName);
  }

  void scheduleInBackgroundForCommittedDocument() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
      calcIndentAdjuster(new DumbProgressIndicator()).run();
    }
    else {
      ReadAction
        .nonBlocking(() -> {
          Runnable indentAdjuster = ProgressIndicatorUtils.withTimeout(INDENT_COMPUTATION_TIMEOUT, () ->
            calcIndentAdjuster(Objects.requireNonNull(ProgressIndicatorProvider.getGlobalProgressIndicator())));
          if (indentAdjuster == null) {
            logTooLongComputation();
            return EmptyRunnable.INSTANCE;
          }
          return indentAdjuster;
        })
        .finishOnUiThread(ModalityState.defaultModalityState(), Runnable::run)
        .withDocumentsCommitted(myProject)
        .submit(BOUNDED_EXECUTOR);
    }
  }

  @NotNull
  static TimeStampedIndentOptions getDefaultIndentOptions(@NotNull PsiFile file, @NotNull Document document) {
    FileType fileType = file.getFileType();
    CodeStyleSettings settings = CodeStyle.getSettings(file);
    return new TimeStampedIndentOptions(settings.getIndentOptions(fileType), document.getModificationStamp());
  }

  
}