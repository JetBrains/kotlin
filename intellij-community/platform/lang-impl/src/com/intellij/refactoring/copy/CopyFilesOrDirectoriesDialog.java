// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.copy;

import com.intellij.CommonBundle;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.ide.util.DirectoryUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.impl.FileChooserUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeChooser;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabelDecorator;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.PathUtilRt;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;

public class CopyFilesOrDirectoriesDialog extends RefactoringDialog implements DumbAware {
  public static final int MAX_PATH_LENGTH = 70;

  @NonNls private static final String COPY = "Copy";
  @NonNls private static final String COPY_OPEN_IN_EDITOR = "Copy.OpenInEditor";
  @NonNls private static final String RECENT_KEYS = "CopyFile.RECENT_KEYS";

  public static String shortenPath(@NotNull VirtualFile file) {
    return StringUtil.shortenPathWithEllipsis(file.getPresentableUrl(), MAX_PATH_LENGTH);
  }

  /**
   * Checkbox enabled via the constructor parameters provides a better UX.
   *
   * @deprecated use {@link RefactoringDialog#RefactoringDialog(Project, boolean, boolean)} instead
   */
  @Deprecated
  public static JCheckBox createOpenInEditorCB() {
    JCheckBox checkBox = new JCheckBox(RefactoringBundle.message("open.copy.in.editor"), PropertiesComponent.getInstance().getBoolean(COPY_OPEN_IN_EDITOR, true));
    checkBox.setMnemonic('o');
    return checkBox;
  }

  /**
   * There's no need so save state explicitly if the constructor parameter is used to create a checkbox.
   *
   * @deprecated use {@link RefactoringDialog#RefactoringDialog(Project, boolean, boolean)} instead
   */
  @Deprecated
  public static void saveOpenInEditorState(boolean selected) {
    PropertiesComponent.getInstance().setValue(COPY_OPEN_IN_EDITOR, String.valueOf(selected));
  }

  private JLabel myInformationLabel;
  private TextFieldWithHistoryWithBrowseButton myTargetDirectoryField;
  private boolean myUnknownFileType = false;

  private EditorTextField myNewNameField;
  private final PsiElement[] myElements;
  private final boolean myShowDirectoryField;
  private final boolean myShowNewNameField;

  private PsiDirectory myTargetDirectory;
  private boolean myFileCopy = false;

  public CopyFilesOrDirectoriesDialog(PsiElement[] elements, @Nullable PsiDirectory defaultTargetDirectory, Project project, boolean doClone) {
    super(project, true, canBeOpenedInEditor(elements));
    myElements = elements;
    myShowDirectoryField = !doClone;
    myShowNewNameField = elements.length == 1;

    if (doClone && elements.length != 1) {
      throw new IllegalArgumentException("wrong number of elements to clone: " + elements.length);
    }

    setTitle(RefactoringBundle.message(doClone ? "copy.files.clone.title" : "copy.files.copy.title"));
    init();

    for (int i = 0; i < elements.length; i++) {
      if (elements[i] instanceof PsiFile) {
        elements[i] = ((PsiFile)elements[i]).getOriginalFile();
      }
    }

    if (elements.length == 1) {
      String text;
      if (elements[0] instanceof PsiFile) {
        PsiFile file = (PsiFile)elements[0];
        VirtualFile vFile = file.getVirtualFile();
        text = RefactoringBundle.message(doClone ? "copy.files.clone.file.0" : "copy.files.copy.file.0", shortenPath(vFile));
        String fileName = vFile.isInLocalFileSystem() ? vFile.getName() : PathUtil.suggestFileName(file.getName(), true, true);
        if (StringUtil.isEmpty(vFile.getExtension()) && ScratchUtil.isScratch(vFile)) {
          FileType type = ObjectUtils.notNull(file.getLanguage().getAssociatedFileType(), file.getFileType());
          fileName = PathUtil.makeFileName(fileName, type.getDefaultExtension());
        }
        myNewNameField.setText(fileName);
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
          selectNameWithoutExtension(dotIdx);
        }
        myTargetDirectory = file.getContainingDirectory();
        myFileCopy = true;
      }
      else {
        VirtualFile vFile = ((PsiDirectory)elements[0]).getVirtualFile();
        text = RefactoringBundle.message(doClone ? "copy.files.clone.directory.0" : "copy.files.copy.directory.0", shortenPath(vFile));
        myNewNameField.setText(vFile.getName());
      }
      myInformationLabel.setText(text);
    }
    else {
      setMultipleElementCopyLabel(elements);
    }

    if (myShowDirectoryField) {
      String targetPath = defaultTargetDirectory == null ? "" : defaultTargetDirectory.getVirtualFile().getPresentableUrl();
      getTargetDirectoryComponent().setText(targetPath);
    }
    validateButtons();
    getRefactorAction().putValue(Action.NAME, CommonBundle.getOkButtonText());
  }

  private static boolean canBeOpenedInEditor(PsiElement[] elements) {
    for (PsiElement element : elements) {
      if (!(element.getContainingFile() instanceof PsiBinaryFile)) {
        return true;
      }
    }
    return false;
  }

  private void selectNameWithoutExtension(int dotIdx) {
    Runnable selectRunnable = () -> {
      Editor editor = myNewNameField.getEditor();
      if (editor != null) {
        editor.getSelectionModel().setSelection(0, dotIdx);
        editor.getCaretModel().moveToOffset(dotIdx);
      }
      else {
        myNewNameField.selectAll();
      }
    };
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(selectRunnable);
  }

  private void setMultipleElementCopyLabel(PsiElement[] elements) {
    boolean allFiles = true;
    boolean allDirectories = true;
    for (PsiElement element : elements) {
      if (element instanceof PsiDirectory) {
        allFiles = false;
      }
      else {
        allDirectories = false;
      }
    }
    if (allFiles) {
      myInformationLabel.setText(RefactoringBundle.message("copy.files.copy.specified.files.label"));
    }
    else if (allDirectories) {
      myInformationLabel.setText(RefactoringBundle.message("copy.files.copy.specified.directories.label"));
    }
    else {
      myInformationLabel.setText(RefactoringBundle.message("copy.files.copy.specified.mixed.label"));
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myShowNewNameField ? myNewNameField : getTargetDirectoryComponent();
  }

  protected TextFieldWithHistory getTargetDirectoryComponent() {
    return myTargetDirectoryField.getChildComponent();
  }

  @Override
  protected JComponent createCenterPanel() {
    return new JPanel(new BorderLayout());
  }

  @Override
  protected JComponent createNorthPanel() {
    myInformationLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true);
    final FormBuilder formBuilder = FormBuilder.createFormBuilder().addComponent(myInformationLabel).addVerticalGap(
      UIUtil.LARGE_VGAP - UIUtil.DEFAULT_VGAP);

    if (myShowNewNameField) {
      myNewNameField = new EditorTextField();
      myNewNameField.addDocumentListener(new DocumentListener() {
        @Override
        public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
          validateButtons();
        }
      });
      formBuilder.addLabeledComponent(RefactoringBundle.message("copy.files.new.name.label"), myNewNameField);
    }

    if (myShowDirectoryField) {
      myTargetDirectoryField = new TextFieldWithHistoryWithBrowseButton();
      myTargetDirectoryField.setTextFieldPreferredWidth(MAX_PATH_LENGTH);
      final List<String> recentEntries = RecentsManager.getInstance(myProject).getRecentEntries(RECENT_KEYS);
      if (recentEntries != null) {
        getTargetDirectoryComponent().setHistory(recentEntries);
      }
      final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      myTargetDirectoryField.addBrowseFolderListener(RefactoringBundle.message("select.target.directory"),
                                                     RefactoringBundle.message("the.file.will.be.copied.to.this.directory"),
                                                     myProject, descriptor,
                                                     TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
      getTargetDirectoryComponent().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          validateButtons();
        }
      });
      formBuilder.addLabeledComponent(RefactoringBundle.message("copy.files.to.directory.label"), myTargetDirectoryField);

      String shortcutText =
        KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));
      formBuilder.addTooltip(RefactoringBundle.message("path.completion.shortcut", shortcutText));
    }

    return formBuilder.getPanel();
  }

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }

  public String getNewName() {
    return myNewNameField != null ? myNewNameField.getText().trim() : null;
  }

  /**
   * @deprecated use {@link #isOpenInEditor()} instead
   */
  @Deprecated
  public boolean openInEditor() {
    return isOpenInEditor();
  }

  @Override
  public boolean isOpenInEditor() {
    return !myUnknownFileType && super.isOpenInEditor();
  }

  @Override
  protected void doAction() {
    if (myShowNewNameField) {
      String newName = getNewName();

      if (newName.length() == 0) {
        Messages.showErrorDialog(myProject, RefactoringBundle.message("no.new.name.specified"), RefactoringBundle.message("error.title"));
        return;
      }

      if (myFileCopy && !PathUtilRt.isValidFileName(newName, false)) {
        Messages.showErrorDialog(myNewNameField, RefactoringBundle.message("name.is.not.a.valid.file.name"));
        return;
      }

      if (myFileCopy && myTargetDirectory != null && isOpenInEditor()) {
        if (FileTypeChooser.getKnownFileTypeOrAssociate(myTargetDirectory.getVirtualFile(), newName, myProject) == null) {
          myUnknownFileType = true;
        }
      }
    }

    if (myShowDirectoryField) {
      final String targetDirectoryName = getTargetDirectoryComponent().getText();

      if (targetDirectoryName.length() == 0) {
        Messages.showErrorDialog(myProject, RefactoringBundle.message("no.target.directory.specified"),
                                 RefactoringBundle.message("error.title"));
        return;
      }

      RecentsManager.getInstance(myProject).registerRecentEntry(RECENT_KEYS, targetDirectoryName);

      CommandProcessor.getInstance().executeCommand(myProject, () -> ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          String path = FileUtil.toSystemIndependentName(targetDirectoryName);
          myTargetDirectory = DirectoryUtil.mkdirs(PsiManager.getInstance(myProject), path);
        }
        catch (IncorrectOperationException ignored) { }
      }), RefactoringBundle.message("create.directory"), null);

      if (myTargetDirectory == null) {
        Messages.showErrorDialog(myProject, RefactoringBundle.message("cannot.create.directory"), RefactoringBundle.message("error.title"));
        return;
      }
      FileChooserUtil.setLastOpenedFile(myProject, myTargetDirectory.getVirtualFile().toNioPath());

      try {
        for (PsiElement element : myElements) {
          MoveFilesOrDirectoriesUtil.checkIfMoveIntoSelf(element, myTargetDirectory);
        }
      }
      catch (IncorrectOperationException e) {
        Messages.showErrorDialog(myProject, e.getMessage(), RefactoringBundle.message("error.title"));
        return;
      }
    }

    closeOKAction();
  }

  @Override
  protected boolean hasPreviewButton() {
    return false;
  }

  @Override
  protected boolean areButtonsValid() {
    if (myShowDirectoryField && getTargetDirectoryComponent().getText().length() == 0) {
      return false;
    }
    if (myShowNewNameField) {
      String newName = getNewName();
      if (newName.length() == 0 || myFileCopy && !PathUtilRt.isValidFileName(newName, false)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected @NotNull String getRefactoringId() {
    return COPY;
  }

  @Override
  protected String getHelpId() {
    return "refactoring.copyClass";
  }
}