// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * @author gregsh
 */
public final class ScratchUtil {
  private ScratchUtil() {
  }

  /**
   * Returns true if a file or a directory is in one of scratch roots: scratch, console, etc.
   * @see RootType
   * @see ScratchFileService
   */
  public static boolean isScratch(@Nullable VirtualFile file) {
    RootType rootType = RootType.forFile(file);
    return rootType != null && !rootType.isHidden();
  }

  public static void updateFileExtension(@NotNull Project project, @Nullable VirtualFile file) throws IOException {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    if (CommandProcessor.getInstance().getCurrentCommand() == null) {
      throw new AssertionError("command required");
    }

    if (file == null) return;
    Language language = LanguageUtil.getLanguageForPsi(project, file);
    FileType prevType = getFileTypeFromName(file);
    FileType currType = language == null ? null : language.getAssociatedFileType();
    if (prevType == currType) return;
    String prevExt = PathUtil.makeFileName("", prevType == null ? "" : prevType.getDefaultExtension());
    String currExt = currType == null ? "" : currType.getDefaultExtension();
    // support multipart extensions like *.blade.php
    String nameWithoutExtension = prevExt.length() > 0 && file.getName().endsWith(prevExt) ?
                                  StringUtil.trimEnd(file.getName(), prevExt) : file.getNameWithoutExtension();
    VirtualFile parent = file.getParent();
    String newName = parent != null ? VfsUtil.getNextAvailableName(parent, nameWithoutExtension, currExt) :
                     PathUtil.makeFileName(nameWithoutExtension, currExt);
    file.rename(ScratchUtil.class, newName);
  }

  public static boolean hasMatchingExtension(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.getExtension() == null) return true;
    FileType expected = getFileTypeFromName(file);
    Language language = LanguageUtil.getLanguageForPsi(project, file);
    FileType actual = language == null ? null : language.getAssociatedFileType();
    if (expected != null && expected == actual) return true;
    String ext = actual == null ? "" : actual.getDefaultExtension();
    return ext.length() > 0 && file.getName().endsWith(ext);
  }

  @Nullable
  private static FileType getFileTypeFromName(@NotNull VirtualFile file) {
    if (file.getExtension() == null) return null;
    FileType result = FileTypeManager.getInstance().getFileTypeByFileName(file.getNameSequence());
    if (result == UnknownFileType.INSTANCE || StringUtil.isEmpty(result.getDefaultExtension())) {
      return null;
    }
    return result;
  }

  @NotNull
  public static String getRelativePath(@NotNull Project project, @NotNull VirtualFile file) {
    RootType rootType = Objects.requireNonNull(RootType.forFile(file));
    String rootPath = ScratchFileService.getInstance().getRootPath(rootType);
    VirtualFile rootFile = LocalFileSystem.getInstance().findFileByPath(rootPath);
    if (rootFile == null || !VfsUtilCore.isAncestor(rootFile, file, false)) {
      throw new AssertionError(file.getPath());
    }
    StringBuilder sb = new StringBuilder();
    for (VirtualFile o = file; !rootFile.equals(o); o = o.getParent()) {
      String part = StringUtil.notNullize(rootType.substituteName(project, o), o.getName());
      if (sb.length() == 0 && part.indexOf('/') > -1) {
        // db console root type adds folder here, trim it
        part = part.substring(part.lastIndexOf('/') + 1);
      }
      sb.insert(0, "/" + part);
    }
    sb.insert(0, rootType.getDisplayName());
    if (sb.charAt(sb.length() - 1) == ']') {
      // db console root type adds [data source name] here, trim it
      int idx = sb.lastIndexOf(" [");
      if (idx > 0 && sb.indexOf("/" + sb.substring(idx + 2, sb.length() - 1) + "/") < idx) {
        sb.setLength(idx);
      }
    }
    return sb.toString();
  }
}
