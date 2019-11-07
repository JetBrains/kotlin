// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeEditor.printing;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class ExportToHTMLManager {
  private static final Logger LOG = Logger.getInstance(ExportToHTMLManager.class);
  private static FileNotFoundException myLastException;

  private ExportToHTMLManager() {
  }

  /**
   * Should be invoked in event dispatch thread
   */
  public static void executeExport(@NotNull final DataContext dataContext) throws FileNotFoundException {
    final PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
    PsiDirectory psiDirectory = null;
    if (psiFile != null) {
      psiDirectory = psiFile.getContainingDirectory();
    }
    else {
      PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      if (psiElement instanceof PsiDirectory) {
        psiDirectory = (PsiDirectory)psiElement;
      }
    }

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    Project project = psiDirectory != null ? psiDirectory.getProject() : editor != null ? editor.getProject() : CommonDataKeys.PROJECT.getData(dataContext);

    String shortFileName = null;
    String directoryName = null;
    if (psiFile != null || psiDirectory != null) {
      if (psiFile != null) {
        shortFileName = psiFile.getVirtualFile().getName();
        if (psiDirectory == null) {
          psiDirectory = psiFile.getContainingDirectory();
        }
      }
      if (psiDirectory != null) {
        directoryName = psiDirectory.getVirtualFile().getPresentableUrl();
      }
    }

    final boolean isSelectedTextEnabled = editor != null && editor.getSelectionModel().hasSelection();

    ExportToHTMLDialog exportToHTMLDialog = new ExportToHTMLDialog(shortFileName, directoryName, isSelectedTextEnabled, project);

    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(project);
    if (exportToHTMLSettings.OUTPUT_DIRECTORY == null) {
      String baseDir = Objects.requireNonNull(project).getBasePath();
      if (baseDir != null) {
        exportToHTMLSettings.OUTPUT_DIRECTORY = baseDir + File.separator + "exportToHTML";
      }
      else {
        exportToHTMLSettings.OUTPUT_DIRECTORY = "";
      }
    }
    exportToHTMLDialog.reset();
    if (!exportToHTMLDialog.showAndGet()) {
      return;
    }
    try {
      exportToHTMLDialog.apply();
    }
    catch (ConfigurationException e) {
      Messages.showErrorDialog(project, e.getMessage(), CommonBundle.getErrorTitle());
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments();
    final String outputDirectoryName = exportToHTMLSettings.OUTPUT_DIRECTORY;
    if (exportToHTMLSettings.getPrintScope() != PrintSettings.PRINT_DIRECTORY) {
      if (psiFile == null || psiFile.getText() == null) {
        return;
      }
      final String dirName = constructOutputDirectory(psiFile, outputDirectoryName);
      HTMLTextPainter textPainter = new HTMLTextPainter(psiFile, project, exportToHTMLSettings.PRINT_LINE_NUMBERS);
      if (exportToHTMLSettings.getPrintScope() == PrintSettings.PRINT_SELECTED_TEXT &&
          editor != null &&
          editor.getSelectionModel().hasSelection()) {
        int firstLine = editor.getDocument().getLineNumber(editor.getSelectionModel().getSelectionStart());
        textPainter.setSegment(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), firstLine);
      }

      try {
        String htmlFile = doPaint(dirName, textPainter, null);
        if (exportToHTMLSettings.OPEN_IN_BROWSER) {
          BrowserUtil.browse(htmlFile);
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
    else {
      myLastException = null;
      ExportRunnable exportRunnable =
        new ExportRunnable(exportToHTMLSettings, psiDirectory, outputDirectoryName, project);
      ProgressManager.getInstance()
        .runProcessWithProgressSynchronously(exportRunnable, CodeEditorBundle.message("export.to.html.title"), true, project);
      if (myLastException != null) {
        throw myLastException;
      }
    }
  }

  @NotNull
  protected static String doPaint(@NotNull String dirName, @NotNull HTMLTextPainter textPainter, @Nullable TreeMap refMap) throws IOException {
    String htmlFile = dirName + File.separator + getHTMLFileName(textPainter.getPsiFile());
    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_8)) {
      textPainter.paint(refMap, writer, true);
    }
    return htmlFile;
  }

  private static boolean exportPsiFile(final PsiFile psiFile,
                                       final String outputDirectoryName,
                                       final Project project,
                                       final HashMap<PsiFile, PsiFile> filesMap) {
    final ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(project);

    if (psiFile instanceof PsiBinaryFile) {
      return true;
    }

    ApplicationManager.getApplication().runReadAction(() -> {
      if (!psiFile.isValid()) {
        return;
      }
      TreeMap<Integer, PsiReference> refMap = null;
      for (PrintOption printOption : PrintOption.EP_NAME.getExtensionList()) {
        final TreeMap<Integer, PsiReference> map = printOption.collectReferences(psiFile, filesMap);
        if (map != null) {
          refMap = new TreeMap<>(map);
        }
      }

      String dirName = constructOutputDirectory(psiFile, outputDirectoryName);
      try {
        doPaint(dirName, new HTMLTextPainter(psiFile, project, exportToHTMLSettings.PRINT_LINE_NUMBERS), refMap);
      }
      catch (FileNotFoundException e) {
        myLastException = e;
      }
      catch (IOException e) {
        LOG.error(e);
      }
    });
    return myLastException == null;
  }

  private static String constructOutputDirectory(PsiFile psiFile, String outputDirectoryName) {
    return constructOutputDirectory(psiFile.getContainingDirectory(), outputDirectoryName);
  }

  private static String constructOutputDirectory(@NotNull final PsiDirectory directory, final String outputDirectoryName) {
    String qualifiedName = PsiDirectoryFactory.getInstance(directory.getProject()).getQualifiedName(directory, false);
    String dirName = outputDirectoryName;
    if(qualifiedName.length() > 0) {
      dirName += File.separator + qualifiedName.replace('.', File.separatorChar);
    }
    File dir = new File(dirName);
    dir.mkdirs();
    return dirName;
  }

  private static void addToPsiFileList(PsiDirectory psiDirectory,
                                       List<? super PsiFile> filesList,
                                       boolean isRecursive,
                                       final String outputDirectoryName) throws FileNotFoundException {
    if (!psiDirectory.isValid()) {
      return;
    }
    PsiFile[] files = psiDirectory.getFiles();
    Collections.addAll(filesList, files);
    generateIndexHtml(psiDirectory, isRecursive, outputDirectoryName);
    if (isRecursive) {
      PsiDirectory[] directories = psiDirectory.getSubdirectories();
      for (PsiDirectory directory : directories) {
        addToPsiFileList(directory, filesList, isRecursive, outputDirectoryName);
      }
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static void generateIndexHtml(final PsiDirectory psiDirectory, final boolean recursive, final String outputDirectoryName) throws FileNotFoundException {
    String indexHtmlName = constructOutputDirectory(psiDirectory, outputDirectoryName) + File.separator + "index.html";
    final String title = PsiDirectoryFactory.getInstance(psiDirectory.getProject()).getQualifiedName(psiDirectory, true);
    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(indexHtmlName), StandardCharsets.UTF_8)) {
      writer.write("<html><head><title>" + title + "</title></head><body>");
      if (recursive) {
        PsiDirectory[] directories = psiDirectory.getSubdirectories();
        for(PsiDirectory directory: directories) {
          writer.write("<a href=\"" + directory.getName() + "/index.html\"><b>" + directory.getName() + "</b></a><br />");
        }
      }
      PsiFile[] files = psiDirectory.getFiles();
      for(PsiFile file: files) {
        if (!(file instanceof PsiBinaryFile)) {
          writer.write("<a href=\"" + getHTMLFileName(file) + "\">" + file.getVirtualFile().getName() + "</a><br />");
        }
      }
      writer.write("</body></html>");
    }
    catch (FileNotFoundException e) {
      throw e;
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static class ExportRunnable implements Runnable {
    private final ExportToHTMLSettings myExportToHTMLSettings;
    private final PsiDirectory myPsiDirectory;
    private final String myOutputDirectoryName;
    private final Project myProject;

    ExportRunnable(ExportToHTMLSettings exportToHTMLSettings,
                          PsiDirectory psiDirectory,
                          String outputDirectoryName,
                          Project project) {
      myExportToHTMLSettings = exportToHTMLSettings;
      myPsiDirectory = psiDirectory;
      myOutputDirectoryName = outputDirectoryName;
      myProject = project;
    }

    @Override
    public void run() {
      ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();

      final ArrayList<PsiFile> filesList = new ArrayList<>();
      final boolean isRecursive = myExportToHTMLSettings.isIncludeSubdirectories();

      ApplicationManager.getApplication().runReadAction(() -> {
        try {
          addToPsiFileList(myPsiDirectory, filesList, isRecursive, myOutputDirectoryName);
        }
        catch (FileNotFoundException e) {
          myLastException = e;
        }
      });
      if (myLastException != null) {
        return;
      }
      HashMap<PsiFile, PsiFile> filesMap = new HashMap<>();
      for (PsiFile psiFile : filesList) {
        filesMap.put(psiFile, psiFile);
      }
      for(int i = 0; i < filesList.size(); i++) {
        PsiFile psiFile = filesList.get(i);
        if(progressIndicator.isCanceled()) {
          return;
        }
        progressIndicator.setText(CodeEditorBundle.message("export.to.html.generating.file.progress", getHTMLFileName(psiFile)));
        progressIndicator.setFraction(((double)i)/filesList.size());
        if (!exportPsiFile(psiFile, myOutputDirectoryName, myProject, filesMap)) {
          return;
        }
      }
      if (myExportToHTMLSettings.OPEN_IN_BROWSER) {
        String dirToShow = myExportToHTMLSettings.OUTPUT_DIRECTORY;
        if (!dirToShow.endsWith(File.separator)) {
          dirToShow += File.separatorChar;
        }
        dirToShow += PsiDirectoryFactory.getInstance(myProject).getQualifiedName(myPsiDirectory, false).replace('.', File.separatorChar);
        BrowserUtil.browse(dirToShow);
      }
    }
  }

  static String getHTMLFileName(PsiFile psiFile) {
    //noinspection HardCodedStringLiteral
    return psiFile.getVirtualFile().getName() + ".html";
  }
}
