// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author gregsh
 *
 * @deprecated use {@link ScratchUtil#isScratch(VirtualFile)}.
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
public class ScratchFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {

  public static final LanguageFileType INSTANCE = new ScratchFileType();

  ScratchFileType() {
    super(PlainTextLanguage.INSTANCE, true);
  }

  @Override
  public boolean isMyFileType(@NotNull VirtualFile file) {
    return ScratchFileService.isInScratchRoot(file instanceof FakeVirtualFile ? file.getParent() : file);
  }

  @NotNull
  @Override
  public String getName() {
    return "Scratch";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Scratch";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return PlainTextFileType.INSTANCE.getIcon();
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }
}
