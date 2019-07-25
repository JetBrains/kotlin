// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

class CreatePackageHandler extends CreateGroupHandler {

  private static final String DELIMITER = ".";
  private static final String REGEX_DELIMITER = Pattern.quote(DELIMITER);

  @NotNull private final PsiDirectory myPackageRoot;
  @NotNull private final String myInitialText;

  CreatePackageHandler(@NotNull Project project, @NotNull PsiDirectory directory) {
    super(project, directory);
    myPackageRoot = getPackageRoot();
    myInitialText = buildInitialText();
  }

  @Override
  public boolean checkInput(String inputString) {
    if (inputString.isEmpty() || inputString.equals(myInitialText)) {
      errorText = null;
      return true;
    }

    if (inputString.endsWith(DELIMITER)) {
      errorText = IdeBundle.message("error.invalid.java.package.name.format");
      return false;
    }

    final PsiDirectoryFactory nameValidator = PsiDirectoryFactory.getInstance(myProject);
    VirtualFile file = myPackageRoot.getVirtualFile();
    errorText = null;

    for (String token : inputString.split(REGEX_DELIMITER)) {
      if (token.isEmpty()) {
        errorText = IdeBundle.message("error.invalid.java.package.name.format");
        return false;
      }

      if (file != null) {
        file = file.findChild(token);
        if (file != null && !file.isDirectory()) {
          errorText = IdeBundle.message("error.file.with.name.already.exists", token);
          return false;
        }
      }

      if (!nameValidator.isValidPackageName(token)) {
        errorText = IdeBundle.message("error.invalid.java.package.name");
      }

      if (FileTypeManager.getInstance().isFileIgnored(token)) {
        errorText = IdeBundle.message("warning.create.package.with.ignored.name");
      }
    }

    if (file != null) {
      errorText = IdeBundle.message("error.package.with.name.already.exists", file.getName());
      return false;
    }

    return true;
  }

  @Override
  public boolean canClose(String packageName) {
    if (packageName.isEmpty() || packageName.equals(myInitialText)) {
      Messages.showMessageDialog(myProject,
                                 IdeBundle.message("error.name.should.be.specified"),
                                 CommonBundle.getErrorTitle(),
                                 Messages.getErrorIcon());
      return false;
    }

    CommandProcessor.getInstance().executeCommand(myProject, () -> ApplicationManager.getApplication().runWriteAction(() -> {
      String dirPath = myPackageRoot.getVirtualFile().getPresentableUrl();
      String actionName = IdeBundle.message("progress.creating.package", dirPath, packageName);
      LocalHistoryAction action = LocalHistory.getInstance().startAction(actionName);
      try {
        createdElement = DirectoryUtil.createSubdirectories(packageName, myPackageRoot, DELIMITER);
      }
      catch (IncorrectOperationException ex) {
        ApplicationManager.getApplication().invokeLater(
          () -> Messages.showMessageDialog(myProject,
                                           CreateElementActionBase.filterMessage(ex.getMessage()),
                                           CommonBundle.getErrorTitle(),
                                           Messages.getErrorIcon())
        );
      }
      finally {
        action.finish();
      }
    }), IdeBundle.message("command.create.package"), null);

    return createdElement != null;
  }

  @NotNull
  @Override
  String getInitialText() {
    return myInitialText;
  }

  @NotNull
  private String buildInitialText() {
    if (myPackageRoot.isEquivalentTo(myDirectory)) return "";

    final String root = myPackageRoot.getVirtualFile().getPath();
    final String current = myDirectory.getVirtualFile().getPath();

    return current.substring(root.length() + 1).replace("/", DELIMITER) + DELIMITER;
  }

  @NotNull
  private PsiDirectory getPackageRoot() {
    final PsiDirectoryFactory manager = PsiDirectoryFactory.getInstance(myProject);
    PsiDirectory directory = myDirectory;
    PsiDirectory parent = directory.getParent();

    while (parent != null && manager.isPackage(parent)) {
      directory = parent;
      parent = parent.getParent();
    }

    return directory;
  }
}
