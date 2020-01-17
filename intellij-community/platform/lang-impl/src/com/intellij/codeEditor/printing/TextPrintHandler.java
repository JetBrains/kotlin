// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import com.intellij.CommonBundle;
import com.intellij.application.options.CodeStyle;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.ide.actions.PrintActionHandler;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsUtil;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TextPrintHandler extends PrintActionHandler {
  private static final Logger LOG = Logger.getInstance(TextPrintHandler.class);

  @Override
  public boolean canPrint(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return false;
    VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    if (file != null && file.isDirectory()) return true;
    return CommonDataKeys.EDITOR.getData(dataContext) != null ||
           !getSelectedPsiFiles(dataContext).isEmpty();
  }

  @Override
  public void print(@NotNull DataContext dataContext) {
    executePrint(dataContext);
  }

  public static void executePrint(DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return;

    PsiDirectory psiDirectory = null;
    PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (psiElement instanceof PsiDirectory) {
      psiDirectory = (PsiDirectory)psiElement;
    }

    PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
    String shortFileName = null;
    String directoryName = null;
    if (psiFile != null || psiDirectory != null) {
      if (psiFile != null) {
        shortFileName = psiFile.getName();
        if (psiDirectory == null) {
          psiDirectory = psiFile.getContainingDirectory();
        }
      }
      if (psiDirectory != null) {
        directoryName = psiDirectory.getVirtualFile().getPresentableUrl();
      }
    }

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    String text = null;
    if (editor != null) {
      if (editor.getSelectionModel().hasSelection()) {
        text = CodeEditorBundle.message("print.selected.text.radio");
      }
      else {
        text = psiFile == null ? "Console text" : null;
      }
    }

    List<PsiFile> psiFiles = getSelectedPsiFiles(dataContext);

    PrintDialog printDialog = new PrintDialog(shortFileName, directoryName, text, psiFiles.size(), project);
    printDialog.reset();
    if (!printDialog.showAndGet()) {
      return;
    }
    printDialog.apply();

    final PageFormat pageFormat = createPageFormat();
    final BasePainter painter;

    PrintSettings printSettings = PrintSettings.getInstance();
    if (printSettings.getPrintScope() == PrintSettings.PRINT_FILE && psiFiles.size() > 1) {
      painter = new MultiFilePainter(psiFiles, printSettings.EVEN_NUMBER_OF_PAGES);
    }
    else if (printSettings.getPrintScope() == PrintSettings.PRINT_DIRECTORY) {
      List<PsiFile> filesList = new ArrayList<>();
      boolean isRecursive = printSettings.isIncludeSubdirectories();
      addToPsiFileList(Objects.requireNonNull(psiDirectory), filesList, isRecursive);
      painter = new MultiFilePainter(filesList, printSettings.EVEN_NUMBER_OF_PAGES);
    }
    else {
      if (psiFile == null && editor == null) return;
      TextPainter textPainter;
      if (psiFile != null) {
        textPainter = initTextPainter(psiFile);
      }
      else {
        textPainter = initTextPainter((DocumentEx)editor.getDocument(), project, generateFileName(dataContext));
      }
      if (textPainter == null) return;

      if (printSettings.getPrintScope() == PrintSettings.PRINT_SELECTED_TEXT &&
          editor != null &&
          editor.getSelectionModel().hasSelection()) {
        textPainter.setSegment(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());
      }
      painter = textPainter;
    }
    executePrintInner(project, pageFormat, painter);
  }

  private static void executePrintInner(Project project, PageFormat pageFormat, BasePainter painter) {
    final PrinterJob printerJob = PrinterJob.getPrinterJob();
    try {
      printerJob.setPrintable(painter, pageFormat);
      if (!printerJob.printDialog()) {
        return;
      }
    }
    catch (Exception e) {
      LOG.warn(e);
    }

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    ProgressManager.getInstance()
      .run(new Task.Backgroundable(project, CodeEditorBundle.message("print.progress"), true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          try {
            painter.setProgress(indicator);
            printerJob.print();
          }
          catch (ProcessCanceledException e) {
            LOG.info("Cancelled");
            printerJob.cancel();
          }
          catch (PrinterException e) {
            LOG.warn(e);
            String message = ObjectUtils.notNull(e.getMessage(), e.getClass().getName());
            Notifications.Bus.notify(new Notification("Print", CommonBundle.getErrorTitle(), message, NotificationType.ERROR));
          }
          catch (Exception e) {
            LOG.error(e);
          }
          finally {
            painter.dispose();
          }
        }
      });
  }

  private static String generateFileName(DataContext dataContext) {
    RunProfile runProfile = dataContext.getData(LangDataKeys.RUN_PROFILE);
    return runProfile == null ? "unknown" : runProfile.getName();
  }

  private static void addToPsiFileList(PsiDirectory psiDirectory, List<? super PsiFile> filesList, boolean isRecursive) {
    PsiFile[] files = psiDirectory.getFiles();
    Collections.addAll(filesList, files);
    if (isRecursive) {
      for (PsiDirectory directory : psiDirectory.getSubdirectories()) {
        if (!Project.DIRECTORY_STORE_FOLDER.equals(directory.getName())) {
          addToPsiFileList(directory, filesList, true);
        }
      }
    }
  }

  static List<PsiFile> getSelectedPsiFiles(DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return Collections.emptyList();
    VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
    if (virtualFiles == null) return Collections.emptyList();
    PsiManager psiManager = PsiManager.getInstance(project);
    List<PsiFile> psiFiles = new ArrayList<>(virtualFiles.length);
    for (VirtualFile virtualFile : virtualFiles) {
      if (virtualFile.isDirectory()) return Collections.emptyList();
      PsiFile psiFile = psiManager.findFile(virtualFile);
      if (psiFile == null || psiFile instanceof PsiBinaryFile) return Collections.emptyList();
      psiFiles.add(psiFile);
    }
    return psiFiles;
  }

  private static PageFormat createPageFormat() {
    PrintSettings printSettings = PrintSettings.getInstance();
    PageFormat pageFormat = new PageFormat();
    Paper paper = new Paper();
    String paperSize = printSettings.PAPER_SIZE;
    double paperWidth = PageSizes.getWidth(paperSize) * 72;
    double paperHeight = PageSizes.getHeight(paperSize) * 72;
    double leftMargin = printSettings.LEFT_MARGIN * 72;
    double rightMargin = printSettings.RIGHT_MARGIN * 72;
    double topMargin = printSettings.TOP_MARGIN * 72;
    double bottomMargin = printSettings.BOTTOM_MARGIN * 72;

    paper.setSize(paperWidth, paperHeight);
    if (printSettings.PORTRAIT_LAYOUT) {
      pageFormat.setOrientation(PageFormat.PORTRAIT);
      paperWidth -= leftMargin + rightMargin;
      paperHeight -= topMargin + bottomMargin;
      paper.setImageableArea(leftMargin, topMargin, paperWidth, paperHeight);
    }
    else {
      pageFormat.setOrientation(PageFormat.LANDSCAPE);
      paperWidth -= topMargin + bottomMargin;
      paperHeight -= leftMargin + rightMargin;
      paper.setImageableArea(rightMargin, topMargin, paperWidth, paperHeight);
    }
    pageFormat.setPaper(paper);
    return pageFormat;
  }

  static TextPainter initTextPainter(final PsiFile psiFile) {
    return ReadAction.compute(() -> doInitTextPainter(psiFile));
  }

  private static TextPainter doInitTextPainter(final PsiFile psiFile) {
    VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) return null;
    DocumentEx doc = (DocumentEx)PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
    if (doc == null) return null;
    EditorHighlighter highlighter = HighlighterFactory.createHighlighter(virtualFile, EditorColorsUtil.getColorSchemeForPrinting(),
                                                                         psiFile.getProject());
    return new TextPainter(doc, highlighter, virtualFile.getPresentableUrl(), virtualFile.getPresentableName(),
                           psiFile.getFileType(), psiFile.getProject(), CodeStyle.getSettings(psiFile));
  }

  private static TextPainter initTextPainter(@NotNull final DocumentEx doc, final @NotNull Project project,
                                             final @NotNull String fileName) {
    final TextPainter[] res = new TextPainter[1];
    ApplicationManager.getApplication().runReadAction(
      () -> {
        res[0] = doInitTextPainter(doc, project, fileName);
      }
    );
    return res[0];
  }

  private static TextPainter doInitTextPainter(@NotNull final DocumentEx doc, @NotNull Project project, @NotNull String fileName) {
    EditorHighlighter highlighter = HighlighterFactory.createHighlighter(EditorColorsUtil.getColorSchemeForPrinting(), "unknown", project);
    return new TextPainter(doc, highlighter, fileName, fileName, FileTypes.PLAIN_TEXT, null, CodeStyle.getSettings(project));
  }
}
