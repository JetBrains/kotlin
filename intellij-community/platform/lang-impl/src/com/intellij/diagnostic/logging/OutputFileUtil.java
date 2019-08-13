/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.diagnostic.logging;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class OutputFileUtil {
  private static final String CONSOLE_OUTPUT_FILE_MESSAGE = "Console output is saving to: ";

  private OutputFileUtil() {
  }

  public static File getOutputFile(@NotNull final RunConfigurationBase configuration) {
    String outputFilePath = configuration.getOutputFilePath();
    if (outputFilePath != null) {
      final String filePath = FileUtil.toSystemDependentName(outputFilePath);
      File file = new File(filePath);
      if (configuration instanceof CommonProgramRunConfigurationParameters && !FileUtil.isAbsolute(filePath)) {
        String directory = ((CommonProgramRunConfigurationParameters)configuration).getWorkingDirectory();
        if (directory != null) {
          file = new File(new File(directory), filePath);
        }
      }
      return file;
    }
    return null;
  }

  public static void attachDumpListener(@NotNull final RunConfigurationBase configuration, @NotNull final ProcessHandler startedProcess, @Nullable final ExecutionConsole console) {
    if (!configuration.isSaveOutputToFile()) {
      return;
    }

    final File file = getOutputFile(configuration);
    if (file != null) {
      startedProcess.addProcessListener(new ProcessAdapter() {
        private PrintStream myOutput;
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
          if (configuration.collectOutputFromProcessHandler() && myOutput != null && outputType != ProcessOutputTypes.SYSTEM) {
            myOutput.print(event.getText());
          }
        }

        @Override
        public void startNotified(@NotNull ProcessEvent event) {
          try {
            myOutput = new PrintStream(new FileOutputStream(file));
          }
          catch (FileNotFoundException ignored) {
          }
          startedProcess.notifyTextAvailable(CONSOLE_OUTPUT_FILE_MESSAGE + FileUtil.toSystemDependentName(file.getAbsolutePath()) + "\n", ProcessOutputTypes.SYSTEM);
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          startedProcess.removeProcessListener(this);
          if (myOutput != null) {
            myOutput.close();
          }
        }
      });
      if (console instanceof ConsoleView) {
        ((ConsoleView)console).addMessageFilter(new ShowOutputFileFilter());
      }
    }
  }

  private static class ShowOutputFileFilter implements Filter {
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
      if (line.startsWith(CONSOLE_OUTPUT_FILE_MESSAGE)) {
        final String filePath = StringUtil.trimEnd(line.substring(CONSOLE_OUTPUT_FILE_MESSAGE.length()), "\n");

        return new Result(entireLength - filePath.length() - 1, entireLength, new HyperlinkInfo() {
          @Override
          public void navigate(final Project project) {
            final VirtualFile file =
              WriteAction
                .compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(filePath)));

            if (file != null) {
              file.refresh(false, false);
              ApplicationManager.getApplication().runReadAction(() -> {
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, file), true);
              });
            }
          }
        });
      }
      return null;
    }
  }
}
