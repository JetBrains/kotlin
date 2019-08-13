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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ReadTask;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.intellij.psi.codeStyle.autodetect.IndentOptionsAdjuster;
import com.intellij.psi.codeStyle.autodetect.IndentOptionsDetectorImpl;
import com.intellij.util.Time;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;


class DetectAndAdjustIndentOptionsTask extends ReadTask {
  private static final Logger LOG = Logger.getInstance(DetectAndAdjustIndentOptionsTask.class);
  private static final int INDENT_COMPUTATION_TIMEOUT = 5 * Time.SECOND;

  private final Document myDocument;
  private final Project myProject;
  private final TimeStampedIndentOptions myOptionsToAdjust;
  private final ExecutorService myExecutor;
  
  private volatile long myComputationStarted = 0;

  DetectAndAdjustIndentOptionsTask(@NotNull Project project,
                                          @NotNull Document document, 
                                          @NotNull TimeStampedIndentOptions toAdjust,
                                          @NotNull ExecutorService executor) {
    myProject = project;
    myDocument = document;
    myOptionsToAdjust = toAdjust;
    myExecutor = executor;
  }
  
  private PsiFile getFile() {
    if (myProject.isDisposed()) {
      return null;
    }
    return PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
  }

  @Nullable
  @Override
  public Continuation performInReadAction(@NotNull ProgressIndicator indicator) throws ProcessCanceledException {
    PsiFile file = getFile();
    if (file == null) {
      return null;
    }
    
    if (!PsiDocumentManager.getInstance(myProject).isCommitted(myDocument)) {
      scheduleInBackgroundForCommittedDocument();
      return null;
    }

    IndentOptionsDetectorImpl detector = new IndentOptionsDetectorImpl(file, indicator);

    myComputationStarted = System.currentTimeMillis();
    IndentOptionsAdjuster adjuster = detector.getIndentOptionsAdjuster();
    
    return new Continuation(adjuster != null ? () -> adjustOptions(adjuster) : EmptyRunnable.INSTANCE);
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

  @Override
  public void onCanceled(@NotNull ProgressIndicator indicator) {
    if (isComputingForTooLong()) {
      logTooLongComputation();
      return;
    }
    
    scheduleInBackgroundForCommittedDocument();
  }

  private void logTooLongComputation() {
    PsiFile file = getFile();
    String fileName = file != null ? file.getName() : "";
    LOG.debug("Indent detection is too long for: " + fileName);
  }

  private boolean isComputingForTooLong() {
    return System.currentTimeMillis() - myComputationStarted > INDENT_COMPUTATION_TIMEOUT;
  }

  public void scheduleInBackgroundForCommittedDocument() {
    if (myProject.isDisposed()) return;
    
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
      Continuation continuation = performInReadAction(new DumbProgressIndicator());
      if (continuation != null) {
        continuation.getAction().run();
      }
    }
    else {
      PsiDocumentManager manager = PsiDocumentManager.getInstance(myProject);
      manager.performForCommittedDocument(myDocument, () -> ProgressIndicatorUtils.scheduleWithWriteActionPriority(myExecutor, this));
    }
  }

  @NotNull
  public static TimeStampedIndentOptions getDefaultIndentOptions(@NotNull PsiFile file, @NotNull Document document) {
    FileType fileType = file.getFileType();
    CodeStyleSettings settings = CodeStyle.getSettings(file);
    return new TimeStampedIndentOptions(settings.getIndentOptions(fileType), document.getModificationStamp());
  }

  
}