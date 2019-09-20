// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.commandLine;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

public class FileSetFormatter extends FileSetProcessor {
  private static final Logger LOG = Logger.getInstance(FileSetFormatter.class);

  private final static String PROJECT_DIR_PREFIX = PlatformUtils.getPlatformPrefix() + ".format.";
  private final static String PROJECT_DIR_SUFFIX = ".tmp";

  private final static String RESULT_MESSAGE_OK = "OK";
  private final static String RESULT_MESSAGE_FAILED = "Failed";
  private final static String RESULT_MESSAGE_NOT_SUPPORTED = "Skipped, not supported.";
  private final static String RESULT_MESSAGE_BINARY_FILE = "Skipped, binary file.";

  private final @NotNull String myProjectUID;
  private @Nullable Project myProject;
  private final MessageOutput myMessageOutput;
  private @NotNull CodeStyleSettings mySettings;

  public FileSetFormatter(@NotNull MessageOutput messageOutput) {
    myMessageOutput = messageOutput;
    mySettings = new CodeStyleSettings();
    myProjectUID = UUID.randomUUID().toString();
  }

  public void setCodeStyleSettings(@NotNull CodeStyleSettings settings) {
    mySettings = settings;
  }

  private void createProject() throws IOException {
    ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
    File projectDir = createProjectDir();
    myProject = projectManager.createProject(myProjectUID, projectDir.getPath());
    if (myProject != null) {
      projectManager.openProject(myProject);
      CodeStyle.setMainProjectSettings(myProject, mySettings);
    }
  }

  private File createProjectDir() throws IOException {
    File tempDir = FileUtil.createTempDirectory(PROJECT_DIR_PREFIX, myProjectUID + PROJECT_DIR_SUFFIX);
    File projectDir = new File(tempDir.getPath() + File.separator + PathMacroUtil.DIRECTORY_STORE_NAME);
    if (projectDir.mkdirs()) {
      return projectDir;
    }
    throw new IOException("Cannot create a temporary project at " + projectDir);
  }

  private void closeProject() {
    if (myProject != null) {
      ProjectManagerEx.getInstanceEx().closeAndDispose(myProject);
    }
  }

  @Override
  public void processFiles() throws IOException {
    createProject();
    if (myProject != null) {
      super.processFiles();
      closeProject();
    }
  }

  @Override
  protected boolean processFile(@NotNull VirtualFile virtualFile) {
    String resultMessage = RESULT_MESSAGE_OK;
    assert myProject != null;
    VfsUtil.markDirtyAndRefresh(false, false, false, virtualFile);
    myMessageOutput.info("Formatting " + virtualFile.getCanonicalPath() + "...");
    if (!virtualFile.getFileType().isBinary()) {
      Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
      if (document != null) {
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        NonProjectFileWritingAccessProvider.allowWriting(Collections.singletonList(virtualFile));
        if (psiFile != null) {
          if (isFormattingSupported(psiFile)) {
            reformatFile(myProject, psiFile, document);
            FileDocumentManager.getInstance().saveDocument(document);
          }
          else {
            resultMessage = RESULT_MESSAGE_NOT_SUPPORTED;
          }
        }
        else {
          LOG.warn("Unable to get a PSI file for " + virtualFile.getPath());
          resultMessage = RESULT_MESSAGE_FAILED;
        }
      }
      else {
        LOG.warn("No document available for " + virtualFile.getPath());
        resultMessage = RESULT_MESSAGE_FAILED;
      }
      FileEditorManager editorManager = FileEditorManager.getInstance(myProject);
      VirtualFile[] openFiles = editorManager.getOpenFiles();
      for (VirtualFile openFile : openFiles) {
        editorManager.closeFile(openFile);
      }
    }
    else {
      resultMessage = RESULT_MESSAGE_BINARY_FILE;
    }
    myMessageOutput.info(resultMessage + "\n");
    return RESULT_MESSAGE_OK.equals(resultMessage);
  }

  private static void reformatFile(@NotNull Project project, @NotNull final PsiFile file, @NotNull Document document) {
    WriteCommandAction.runWriteCommandAction(project, () -> {
      CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
      codeStyleManager.reformatText(file, 0, file.getTextLength());
      PsiDocumentManager.getInstance(project).commitDocument(document);
    });
  }

  private static boolean isFormattingSupported(@NotNull PsiFile file) {
    return LanguageFormatting.INSTANCE.forContext(file) != null;
  }
}
