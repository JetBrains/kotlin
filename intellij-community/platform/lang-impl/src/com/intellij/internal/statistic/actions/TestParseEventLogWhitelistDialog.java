// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions;

import com.intellij.ide.scratch.RootType;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.internal.statistic.eventLog.*;
import com.intellij.internal.statistic.service.fus.FUSWhitelist;
import com.intellij.internal.statistic.service.fus.FUStatisticsWhiteListGroupsService;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.command.WriteCommandAction.writeCommandAction;

public class TestParseEventLogWhitelistDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(TestParseEventLogWhitelistDialog.class);
  private static final int IN_DIVIDER_LOCATION = 650;
  private static final int IN_OUT_DIVIDER_LOCATION = 300;

  private JPanel myMainPanel;
  private JPanel myWhitelistPanel;
  private JPanel myResultPanel;
  private JEditorPane myEventLogPanel;
  private JSplitPane myInputDataSplitPane;
  private JSplitPane myInputOutputSplitPane;

  private final Project myProject;
  private final EditorEx myWhitelistEditor;
  private final EditorEx myResultEditor;
  private final List<PsiFile> myTempFiles = new ArrayList<>();

  protected TestParseEventLogWhitelistDialog(@NotNull Project project, @Nullable Editor selectedEditor) {
    super(project);
    myProject = project;
    setOKButtonText("&Filter Event Log");
    setCancelButtonText("&Close");
    Disposer.register(myProject, getDisposable());
    VirtualFile selectedFile = selectedEditor == null ? null : FileDocumentManager.getInstance().getFile(selectedEditor.getDocument());
    setTitle(selectedFile == null ? "Event Log Filter" : "Event Log Filter by: " + selectedFile.getName());
    myWhitelistEditor = initEditor(selectedEditor, "event-log-whitelist", "{\"groups\":[]}");
    myWhitelistEditor.getSettings().setLineMarkerAreaShown(false);

    myResultEditor = initEditor(null, "event-log-filter-result", "{}");
    myResultEditor.getSettings().setLineMarkerAreaShown(false);

    init();
    if (selectedEditor != null) {
      doOKAction();

      ApplicationManager.getApplication().invokeLater(() -> {
        IdeFocusManager.getGlobalInstance()
          .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myWhitelistEditor.getContentComponent(), true));
        myWhitelistEditor.getCaretModel().moveToOffset(selectedEditor.getCaretModel().getOffset());
        myWhitelistEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
      }, ModalityState.stateForComponent(myMainPanel));
    }
  }

  @NotNull
  private EditorEx initEditor(@Nullable Editor selectedEditor, @NotNull String fileName, @NotNull String templateText) {
    if (selectedEditor != null) {
      return (EditorEx)EditorFactory.getInstance().createEditor(selectedEditor.getDocument(), myProject);
    }

    final PsiFile file = createTempFile(myProject, fileName, templateText);
    assert file != null;

    myTempFiles.add(file);
    Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);
    if (document == null) {
      document = EditorFactory.getInstance().createDocument(templateText);
    }

    final EditorEx editor = (EditorEx)EditorFactory.getInstance().createEditor(document, myProject, file.getVirtualFile(), false);
    editor.setFile(file.getVirtualFile());
    return editor;
  }

  @Nullable
  private static PsiFile createTempFile(@NotNull Project project, @NotNull String filename, @NotNull String request) {
    final String fileName = PathUtil.makeFileName(filename, "json");
    try {
      final ThrowableComputable<PsiFile, Exception> computable = () -> {
        final ScratchFileService fileService = ScratchFileService.getInstance();
        final VirtualFile file =
          fileService.findFile(RootType.findById("scratches"), fileName, ScratchFileService.Option.create_if_missing);

        fileService.getScratchesMapping().setMapping(file, Language.findLanguageByID("JSON"));
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        final Document document = psiFile != null ? PsiDocumentManager.getInstance(project).getDocument(psiFile) : null;
        if (document == null) {
          return null;
        }

        document.insertString(document.getTextLength(), request);
        PsiDocumentManager.getInstance(project).commitDocument(document);
        return psiFile;
      };

      return writeCommandAction(project)
        .withName("Creating temp JSON file for event log")
        .withGlobalUndo().shouldRecordActionForActiveDocument(false)
        .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
        .compute(computable);
    }
    catch (Exception e) {
      // ignore
    }
    return null;
  }

  @Override
  protected void init() {
    configEditorPanel(myProject, myWhitelistPanel, myWhitelistEditor);
    configEditorPanel(myProject, myResultPanel, myResultEditor);

    myInputDataSplitPane.setDividerLocation(IN_DIVIDER_LOCATION);
    myInputOutputSplitPane.setDividerLocation(IN_OUT_DIVIDER_LOCATION);
    super.init();
  }

  private static void configEditorPanel(@NotNull Project project, @NotNull JPanel panel, @NotNull EditorEx editor) {
    panel.setLayout(new BorderLayout());
    panel.add(editor.getComponent(), BorderLayout.CENTER);

    editor.getSettings().setFoldingOutlineShown(false);
    final FileType fileType = FileTypeManager.getInstance().findFileTypeByName("JSON");
    final LightVirtualFile lightFile = new LightVirtualFile("Dummy.json", fileType, "");

    EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, lightFile);
    try {
      editor.setHighlighter(highlighter);
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
  }

  @Override
  @NotNull
  protected String getDimensionServiceKey() {
    return TestParseEventLogWhitelistDialog.class.getCanonicalName();
  }

  @SuppressWarnings("TestOnlyProblems")
  @Override
  protected void doOKAction() {
    myWhitelistEditor.getSelectionModel().removeSelection();
    myResultEditor.getSelectionModel().removeSelection();
    updateResultRequest("{}");

    final FUSWhitelist whitelist = FUStatisticsWhiteListGroupsService.parseApprovedGroups(myWhitelistEditor.getDocument().getText());
    try {
      final String parsed = parseLogAndFilter(new LogEventWhitelistFilter(whitelist), myEventLogPanel.getText());
      updateResultRequest(parsed.trim());
    }
    catch (IOException | ParseEventLogWhitelistException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), "Failed Applying Whitelist to Event Log");
    }
  }

  private void updateResultRequest(@NotNull String text) {
    writeCommandAction(myProject).run(() -> {
      final DocumentEx document = myResultEditor.getDocument();
      document.setText(text);
      PsiDocumentManager.getInstance(myProject).commitDocument(myResultEditor.getDocument());
    });
  }

  @NotNull
  private static String parseLogAndFilter(@NotNull LogEventFilter filter, @NotNull String text)
    throws IOException, ParseEventLogWhitelistException {
    final File log = FileUtil.createTempFile("feature-event-log", ".log");
    try {
      FileUtil.writeToFile(log, text);
      final LogEventRecordRequest request = LogEventRecordRequest.Companion.create(log, "FUS", filter, true);
      if (request == null) {
        throw new ParseEventLogWhitelistException("Failed parsing event log");
      }
      return LogEventSerializer.INSTANCE.toString(request);
    }
    finally {
      FileUtil.delete(log);
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myEventLogPanel;
  }

  @Override
  public void dispose() {
    writeCommandAction(myProject).run(() -> {
      for (PsiFile file : myTempFiles) {
        try {
          file.delete();
        }
        catch (IncorrectOperationException e) {
          LOG.warn(e);
        }
      }
    });

    if (!myWhitelistEditor.isDisposed()) {
      EditorFactory.getInstance().releaseEditor(myWhitelistEditor);
    }

    if (!myResultEditor.isDisposed()) {
      EditorFactory.getInstance().releaseEditor(myResultEditor);
    }
    super.dispose();
  }

  public static class ParseEventLogWhitelistException extends Exception {
    public ParseEventLogWhitelistException(String s) {
      super(s);
    }
  }
}
