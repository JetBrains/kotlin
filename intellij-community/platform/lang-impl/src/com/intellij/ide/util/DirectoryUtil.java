/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.ide.util;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.StringTokenizer;

public class DirectoryUtil {
  private DirectoryUtil() {
  }


  /**
   * Creates the directory with the given path via PSI, including any
   * necessary but nonexistent parent directories. Must be run in write action.
   * @param path directory path in the local file system; separators must be '/'
   * @return true if path exists or has been created as the result of this method call; false otherwise
   */
  public static PsiDirectory mkdirs(PsiManager manager, String path) throws IncorrectOperationException{
    if (File.separatorChar != '/') {
      if (path.indexOf(File.separatorChar) != -1) {
        throw new IllegalArgumentException("separators must be '/'; path is " + path);
      }
    }

    String existingPath = path;

    PsiDirectory directory = null;

    // find longest existing path
    while (existingPath.length() > 0) {
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(existingPath);
      if (file != null) {
        directory = manager.findDirectory(file);
        if (directory == null) {
          return null;
        }
        break;
      }

      if (StringUtil.endsWithChar(existingPath, '/')) {
        existingPath = existingPath.substring(0, existingPath.length() - 1);
        if (SystemInfo.isWindows && existingPath.length() == 2 && existingPath.charAt(1) == ':') {
          return null;
        }
      }

      int index = existingPath.lastIndexOf('/');
      if (index == -1) {
        // nothing to do more
        return null;
      }

      existingPath = existingPath.substring(0, index);
    }

    if (directory == null) {
      return null;
    }

    if (existingPath.equals(path)) {
      return directory;
    }

    String postfix = path.substring(existingPath.length() + 1);
    StringTokenizer tokenizer = new StringTokenizer(postfix, "/");
    while (tokenizer.hasMoreTokens()) {
      directory = directory.createSubdirectory(tokenizer.nextToken());
    }

    return directory;
  }

  public static PsiDirectory createSubdirectories(final String subDirName, PsiDirectory baseDirectory, final String delim) throws IncorrectOperationException {
    StringTokenizer tokenizer = new StringTokenizer(subDirName, delim);
    PsiDirectory dir = baseDirectory;
    boolean firstToken = true;
    while (tokenizer.hasMoreTokens()) {
      String dirName = tokenizer.nextToken();
      if (tokenizer.hasMoreTokens()) {
        if (firstToken && "~".equals(dirName)) {
          final VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
          if (userHomeDir == null) throw new IncorrectOperationException("User home directory not found");
          final PsiDirectory directory1 = baseDirectory.getManager().findDirectory(userHomeDir);
          if (directory1 == null) throw new IncorrectOperationException("User home directory not found");
          dir = directory1;
          continue;
        }
        else if ("..".equals(dirName)) {
          dir = dir.getParentDirectory();
          if (dir == null) throw new IncorrectOperationException("Not a valid directory");
          continue;
        }
        else if (".".equals(dirName)) {
          continue;
        }
        PsiDirectory existingDir = dir.findSubdirectory(dirName);
        if (existingDir != null) {
          dir = existingDir;
          continue;
        }
      }
      dir = dir.createSubdirectory(dirName);
      firstToken = false;
    }
    return dir;
  }
}
