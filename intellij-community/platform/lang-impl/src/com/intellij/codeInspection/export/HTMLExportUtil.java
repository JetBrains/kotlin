// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.export;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ThrowableConsumer;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HTMLExportUtil {
  public static void writeFile(@NotNull Path dir,
                               @NotNull String fileName,
                               @NotNull Project project,
                               @NotNull ThrowableConsumer<? super Writer, ? extends IOException> writerConsumer) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    Path fullPath = dir.resolve(fileName);

    if (indicator != null) {
      ProgressManager.checkCanceled();
      indicator.setText(InspectionsBundle.message("inspection.export.generating.html.for", fullPath.toString()));
    }

    try {
      Files.createDirectories(dir);
    }
    catch (IOException e) {
      showErrorMessage("Can't create dir", dir, project);
      return;
    }

    try (BufferedWriter writer = Files.newBufferedWriter(fullPath)) {
      writerConsumer.consume(writer);
    }
    catch (AccessDeniedException e) {
      showErrorMessage("Permission denied", fullPath, project);
    }
    catch (IOException e) {
      showErrorMessage(String.valueOf(e.getCause()), fullPath, project);
    }
  }

  private static void showErrorMessage(@NotNull String message,
                                       @NotNull Path file,
                                       @NotNull Project project) {
    Runnable showError = () -> Messages.showMessageDialog(
      project,
      InspectionsBundle.message("inspection.export.error.writing.to", file.toString(), message),
      InspectionsBundle.message("inspection.export.results.error.title"),
      Messages.getErrorIcon()
    );
    ApplicationManager.getApplication().invokeLater(showError, ModalityState.NON_MODAL);
    throw new ProcessCanceledException();
  }
}
