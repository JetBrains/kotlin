// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.DirectoryUtil;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.StringTokenizer;

public class CreateDirectoryOrPackageHandler implements InputValidatorEx {
  @Nullable private final Project myProject;
  @NotNull private final PsiDirectory myDirectory;
  private final boolean myIsDirectory;
  @Nullable private PsiFileSystemItem myCreatedElement = null;
  @NotNull private final String myDelimiters;
  @Nullable private final Component myDialogParent;
  private String myErrorText;

  public CreateDirectoryOrPackageHandler(@Nullable Project project,
                                         @NotNull PsiDirectory directory,
                                         boolean isDirectory,
                                         @NotNull final String delimiters) {
    this(project, directory, isDirectory, delimiters, null);
  }

  public CreateDirectoryOrPackageHandler(@Nullable Project project,
                                         @NotNull PsiDirectory directory,
                                         boolean isDirectory,
                                         @NotNull final String delimiters,
                                         @Nullable Component dialogParent) {
    myProject = project;
    myDirectory = directory;
    myIsDirectory = isDirectory;
    myDelimiters = delimiters;
    myDialogParent = dialogParent;
  }

  @Override
  public boolean checkInput(String inputString) {
    final StringTokenizer tokenizer = new StringTokenizer(inputString, myDelimiters);
    VirtualFile vFile = myDirectory.getVirtualFile();
    boolean firstToken = true;
    while (tokenizer.hasMoreTokens()) {
      final String token = tokenizer.nextToken();
      if (!tokenizer.hasMoreTokens() && (token.equals(".") || token.equals(".."))) {
        myErrorText = IdeBundle.message("error.invalid.directory.name", token);
        return false;
      }
      if (vFile != null) {
        if (firstToken && "~".equals(token)) {
          final VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
          if (userHomeDir == null) {
            myErrorText = IdeBundle.message("error.user.home.directory.not.found");
            return false;
          }
          vFile = userHomeDir;
        }
        else if ("..".equals(token)) {
          final VirtualFile parent = vFile.getParent();
          if (parent == null) {
            myErrorText = IdeBundle.message("error.invalid.directory", vFile.getPresentableUrl() + File.separatorChar + "..");
            return false;
          }
          vFile = parent;
        }
        else if (!".".equals(token)){
          final VirtualFile child = vFile.findChild(token);
          if (child != null) {
            if (!child.isDirectory()) {
              myErrorText = IdeBundle.message("error.file.with.name.already.exists", token);
              return false;
            }
            else if (!tokenizer.hasMoreTokens()) {
              myErrorText = IdeBundle.message("error.directory.with.name.already.exists", token);
              return false;
            }
          }
          vFile = child;
        }
      }
      if (FileTypeManager.getInstance().isFileIgnored(token)) {
        myErrorText = myIsDirectory ? IdeBundle.message("warning.create.directory.with.ignored.name", token)
                                    : IdeBundle.message("warning.create.package.with.ignored.name", token);
        return true;
      }
      if (!myIsDirectory && token.length() > 0 && !PsiDirectoryFactory.getInstance(myProject).isValidPackageName(token)) {
        myErrorText = IdeBundle.message("error.invalid.java.package.name");
        return true;
      }
      firstToken = false;
    }
    myErrorText = null;
    return true;
  }

  @Override
  public String getErrorText(String inputString) {
    return myErrorText;
  }

  @Override
  public boolean canClose(final String subDirName) {

    if (subDirName.length() == 0) {
      showErrorDialog(IdeBundle.message("error.name.should.be.specified"));
      return false;
    }

    final boolean multiCreation = StringUtil.containsAnyChar(subDirName, myDelimiters);
    if (!multiCreation) {
      try {
        myDirectory.checkCreateSubdirectory(subDirName);
      }
      catch (IncorrectOperationException ex) {
        showErrorDialog(CreateElementActionBase.filterMessage(ex.getMessage()));
        return false;
      }
    }

    final Boolean createFile = suggestCreatingFileInstead(subDirName);
    if (createFile == null) {
      return false;
    }

    doCreateElement(subDirName, createFile);

    return myCreatedElement != null;
  }

  @Nullable
  private Boolean suggestCreatingFileInstead(String subDirName) {
    Boolean createFile = false;
    if (StringUtil.countChars(subDirName, '.') == 1 && Registry.is("ide.suggest.file.when.creating.filename.like.directory")) {
      FileType fileType = findFileTypeBoundToName(subDirName);
      if (fileType != null) {
        String message = LangBundle.message("dialog.message.name.you.entered", subDirName);
        int ec = Messages.showYesNoCancelDialog(myProject, message,
                                                LangBundle.message("dialog.title.file.name.detected"),
                                                LangBundle.message("button.yes.create.file"),
                                                LangBundle.message("button.no.create", myIsDirectory ?
                                                                                       LangBundle.message("button.no.create.directory") :
                                                                                       LangBundle.message("button.no.create.package")),
                                                CommonBundle.getCancelButtonText(),
                                                fileType.getIcon());
        if (ec == Messages.CANCEL) {
          createFile = null;
        }
        if (ec == Messages.YES) {
          createFile = true;
        }
      }
    }
    return createFile;
  }

  @Nullable
  public static FileType findFileTypeBoundToName(String name) {
    FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(name);
    return fileType instanceof UnknownFileType ? null : fileType;
  }

  private void doCreateElement(final String subDirName, final boolean createFile) {
    Runnable command = () -> {
      final Runnable run = () -> {
        String dirPath = myDirectory.getVirtualFile().getPresentableUrl();
        String actionName = IdeBundle.message("progress.creating.directory", dirPath, File.separator, subDirName);
        LocalHistoryAction action = LocalHistory.getInstance().startAction(actionName);
        try {
          if (createFile) {
            CreateFileAction.MkDirs mkdirs = new CreateFileAction.MkDirs(subDirName, myDirectory);
            myCreatedElement = mkdirs.directory.createFile(mkdirs.newName);
          } else {
            createDirectories(subDirName);
          }
        }
        catch (final IncorrectOperationException ex) {
          ApplicationManager.getApplication().invokeLater(() -> showErrorDialog(CreateElementActionBase.filterMessage(ex.getMessage())));
        }
        finally {
          action.finish();
        }
      };
      ApplicationManager.getApplication().runWriteAction(run);
    };
    CommandProcessor.getInstance().executeCommand(myProject, command, createFile ? IdeBundle.message("command.create.file") 
                                                                                 : myIsDirectory
                                                                      ? IdeBundle.message("command.create.directory")
                                                                      : IdeBundle.message("command.create.package"), null);
  }

  private void showErrorDialog(String message) {
    String title = CommonBundle.getErrorTitle();
    Icon icon = Messages.getErrorIcon();
    if (myDialogParent != null) {
      Messages.showMessageDialog(myDialogParent, message, title, icon);
    }
    else {
      Messages.showMessageDialog(myProject, message, title, icon);
    }
  }

  protected void createDirectories(String subDirName) {
    myCreatedElement = DirectoryUtil.createSubdirectories(subDirName, myDirectory, myDelimiters);
  }

  @Nullable
  public PsiFileSystemItem getCreatedElement() {
    return myCreatedElement;
  }
}
