// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.lang;

import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileSystemItemUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;

/**
 * @author ven
 */
public class ExtractIncludeDialog extends DialogWrapper {
  private static final String REFACTORING_NAME = RefactoringBundle.message("extractIncludeFile.name");

  private final PsiDirectory myCurrentDirectory;
  protected final String myExtension;

  protected TextFieldWithBrowseButton myTargetDirectoryField;
  private JTextField myNameField;
  protected JLabel myTargetDirLabel;

  public ExtractIncludeDialog(final PsiDirectory currentDirectory, final String extension) {
    super(true);
    myCurrentDirectory = currentDirectory;
    myExtension = extension;
    setTitle(REFACTORING_NAME);
    init();
  }

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }

  private PsiDirectory myTargetDirectory;

  public String getTargetFileName () {
    String name = myNameField.getText().trim();
    return name.contains(".") ? name: name + "." + myExtension;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new VerticalFlowLayout());

    JLabel nameLabel = new JLabel();
    panel.add(nameLabel);

    myNameField = new JTextField();
    nameLabel.setLabelFor(myNameField);
    myNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        validateOKButton();
      }
    });
    panel.add(myNameField);
    nameLabel.setText(getNameLabel());

    myTargetDirLabel = new JLabel();
    panel.add(myTargetDirLabel);

    myTargetDirectoryField = new TextFieldWithBrowseButton();
    myTargetDirectoryField.setText(myCurrentDirectory.getVirtualFile().getPresentableUrl());
    myTargetDirectoryField.addBrowseFolderListener(RefactoringBundle.message("select.target.directory"),
                                                   RefactoringBundle.message("select.target.directory.description"),
                                                   null, FileChooserDescriptorFactory.createSingleFolderDescriptor());

    myTargetDirLabel.setText(RefactoringBundle.message("extract.to.directory"));
    panel.add(myTargetDirectoryField);

    myTargetDirectoryField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(@NotNull DocumentEvent event) {
        validateOKButton();
      }
    });

    validateOKButton();

    return panel;
  }

  protected String getNameLabel() {
    return RefactoringBundle.message("name.for.extracted.include.file", myExtension);
  }

  private void validateOKButton() {
    final String fileName = myNameField.getText().trim();
    setOKActionEnabled(myTargetDirectoryField.getText().trim().length() > 0 &&
                       fileName.length() > 0 && fileName.indexOf(File.separatorChar) < 0 &&
                       !StringUtil.containsAnyChar(fileName, "*?><\":;|") && fileName.indexOf(".") != 0);
  }

  private static boolean isFileExist(@NotNull final String directory, @NotNull final String fileName) {
    return LocalFileSystem.getInstance().findFileByIoFile(new File(directory, fileName)) != null;
  }

  @Override
  protected void doOKAction() {
    final Project project = myCurrentDirectory.getProject();

    final String directoryName = myTargetDirectoryField.getText().replace(File.separatorChar, '/');
    final String targetFileName = getTargetFileName();

    if (isFileExist(directoryName, targetFileName)) {
      Messages.showErrorDialog(project, RefactoringBundle.message("file.already.exist", targetFileName), RefactoringBundle.message("file.already.exist.title"));
      return;
    }

    final FileType type = FileTypeChooser.getKnownFileTypeOrAssociate(targetFileName);
    if (type == null) {
      return;
    }

    CommandProcessor.getInstance().executeCommand(project, () -> {
      final Runnable action = () -> {
        try {
          PsiDirectory targetDirectory = DirectoryUtil.mkdirs(PsiManager.getInstance(project), directoryName);
          assert targetDirectory != null : directoryName;
          targetDirectory.checkCreateFile(targetFileName);
          String webPath = PsiFileSystemItemUtil.findRelativePath(myCurrentDirectory, targetDirectory);
          myTargetDirectory = webPath == null ? null : targetDirectory;
        }
        catch (IncorrectOperationException e) {
          CommonRefactoringUtil.showErrorMessage(REFACTORING_NAME, e.getMessage(), null, project);
        }
      };
      ApplicationManager.getApplication().runWriteAction(action);
    }, RefactoringBundle.message("create.directory"), null);
    if (myTargetDirectory == null) return;
    super.doOKAction();
  }

  @Override
  protected String getHelpId() {
    return ExtractIncludeFileBase.HELP_ID;
  }

  protected void hideTargetDirectory() {
    myTargetDirectoryField.setVisible(false);
    myTargetDirLabel.setVisible(false);
  }
}