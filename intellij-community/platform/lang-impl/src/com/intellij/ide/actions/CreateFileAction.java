// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.ex.FileTypeChooser;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.StringTokenizer;

public class CreateFileAction extends CreateElementActionBase implements DumbAware {

  public CreateFileAction() {
    super(ActionsBundle.message("action.NewFile.text"), IdeBundle.message("action.create.new.file.description"), AllIcons.FileTypes.Text);
  }

  public CreateFileAction(@Nls(capitalization = Nls.Capitalization.Title) String text,
                          @Nls(capitalization = Nls.Capitalization.Sentence) String description,
                          final Icon icon) {
    super(text, description, icon);
  }

  @Override
  public boolean isDumbAware() {
    return CreateFileAction.class.equals(getClass());
  }

  @Override
  @NotNull
  protected PsiElement[] invokeDialog(final Project project, PsiDirectory directory) {
    MyInputValidator validator = new MyValidator(project, directory);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      try {
        return validator.create("test");
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    else {
      Messages.showInputDialog(project, IdeBundle.message("prompt.enter.new.file.name"),
                               IdeBundle.message("title.new.file"), null, null, validator);
      return validator.getCreatedElements();
    }
  }

  @Override
  @NotNull
  protected PsiElement[] create(@NotNull String newName, PsiDirectory directory) throws Exception {
    MkDirs mkdirs = new MkDirs(newName, directory);
    return new PsiElement[]{WriteAction.compute(() -> mkdirs.directory.createFile(getFileName(mkdirs.newName)))};
  }

  public static PsiDirectory findOrCreateSubdirectory(@NotNull PsiDirectory parent, @NotNull String subdirName) {
    final PsiDirectory sub = parent.findSubdirectory(subdirName);
    return sub == null ? WriteAction.compute(() -> parent.createSubdirectory(subdirName)) : sub;
  }

  public static class MkDirs {
    public final String newName;
    public final PsiDirectory directory;

    public MkDirs(@NotNull String newName, @NotNull PsiDirectory directory) {
      if (SystemInfo.isWindows) {
        newName = newName.replace('\\', '/');
      }
      if (newName.contains("/")) {
        final List<String> subDirs = StringUtil.split(newName, "/");
        newName = subDirs.remove(subDirs.size() - 1);
        boolean firstToken = true;
        for (String dir : subDirs) {
          if (firstToken && "~".equals(dir)) {
            final VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
            if (userHomeDir == null) throw new IncorrectOperationException("User home directory not found");
            final PsiDirectory directory1 = directory.getManager().findDirectory(userHomeDir);
            if (directory1 == null) throw new IncorrectOperationException("User home directory not found");
            directory = directory1;
          }
          else if ("..".equals(dir)) {
            final PsiDirectory parentDirectory = directory.getParentDirectory();
            if (parentDirectory == null) throw new IncorrectOperationException("Not a valid directory");
            directory = parentDirectory;
          }
          else if (!".".equals(dir)){
            directory = findOrCreateSubdirectory(directory, dir);
          }
          firstToken = false;
        }
      }

      this.newName = newName;
      this.directory = directory;
    }
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName) {
    return IdeBundle.message("progress.creating.file", directory.getVirtualFile().getPresentableUrl(), File.separator, newName);
  }

  @Override
  protected String getErrorTitle() {
    return IdeBundle.message("title.cannot.create.file");
  }

  @Override
  protected String getCommandName() {
    return IdeBundle.message("command.create.file");
  }

  protected String getFileName(String newName) {
    if (getDefaultExtension() == null || FileUtilRt.getExtension(newName).length() > 0) {
      return newName;
    }
    return newName + "." + getDefaultExtension();
  }

  @Nullable
  protected String getDefaultExtension() {
    return null;
  }

  protected class MyValidator extends MyInputValidator implements InputValidatorEx {
    private String myErrorText;

    public MyValidator(Project project, PsiDirectory directory){
      super(project, directory);
    }

    @Override
    public boolean checkInput(String inputString) {
      final StringTokenizer tokenizer = new StringTokenizer(inputString, "\\/");
      VirtualFile vFile = getDirectory().getVirtualFile();
      boolean firstToken = true;
      while (tokenizer.hasMoreTokens()) {
        final String token = tokenizer.nextToken();
        if ((token.equals(".") || token.equals("..")) && !tokenizer.hasMoreTokens()) {
          myErrorText = "Can't create file with name '" + token + "'";
          return false;
        }
        if (vFile != null) {
          if (firstToken && "~".equals(token)) {
            final VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
            if (userHomeDir == null) {
              myErrorText = "User home directory not found";
              return false;
            }
            vFile = userHomeDir;
          }
          else if ("..".equals(token)) {
            vFile = vFile.getParent();
            if (vFile == null) {
              myErrorText = "Not a valid directory";
              return false;
            }
          }
          else if (!".".equals(token)){
            final VirtualFile child = vFile.findChild(token);
            if (child != null) {
              if (!child.isDirectory()) {
                myErrorText = "A file with name '" + token + "' already exists";
                return false;
              }
              else if (!tokenizer.hasMoreTokens()) {
                myErrorText = "A directory with name '" + token + "' already exists";
                return false;
              }
            }
            vFile = child;
          }
        }
        if (FileTypeManager.getInstance().isFileIgnored(getFileName(token))) {
          myErrorText = "'" + token + "' is an ignored name (Settings | Editor | File Types | Ignore files and folders)";
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
    public PsiElement[] create(@NotNull String newName) throws Exception {
      return super.create(newName);
    }

    @Override
    public boolean canClose(final String inputString) {
      if (inputString.length() == 0) {
        return super.canClose(inputString);
      }

      final PsiDirectory psiDirectory = getDirectory();

      final Project project = psiDirectory.getProject();
      FileType fileType = FileTypeChooser.getKnownFileTypeOrAssociate(psiDirectory.getVirtualFile(), getFileName(inputString), project);
      if (fileType == null) return false;
      return super.canClose(getFileName(inputString));
    }
  }
}
