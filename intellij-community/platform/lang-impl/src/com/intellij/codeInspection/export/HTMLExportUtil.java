/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInspection.export;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HTMLExportUtil {
  public static void writeFile(final String folder, @NonNls final String fileName, CharSequence buf, final Project project) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    final File fullPath = new File(folder + File.separator + fileName);

    if (indicator != null) {
      ProgressManager.checkCanceled();
      indicator.setText(InspectionsBundle.message("inspection.export.generating.html.for", fullPath.getAbsolutePath()));
    }

    final File dir = fullPath.getParentFile();
    if (!dir.exists() && !dir.mkdirs()) {
      showErrorMessage("Can't create dir", dir, project);
      return;
    }
    if (!dir.canWrite() && !fullPath.canWrite()) {
      showErrorMessage("Permission denied", fullPath, project);
      return;
    }
    try (FileWriter writer = new FileWriter(fullPath, false)) {
      writer.write(buf.toString().toCharArray());
    }
    catch (IOException e) {
      showErrorMessage(String.valueOf(e.getCause()), fullPath, project);
    }
  }

  private static void showErrorMessage(@NotNull String message,
                                       @NotNull File file,
                                       @NotNull Project project) {
    Runnable showError = () -> Messages.showMessageDialog(
      project,
      InspectionsBundle.message("inspection.export.error.writing.to", file.getAbsolutePath(), message),
      InspectionsBundle.message("inspection.export.results.error.title"),
      Messages.getErrorIcon()
    );
    ApplicationManager.getApplication().invokeLater(showError, ModalityState.NON_MODAL);
    throw new ProcessCanceledException();
  }
}
