// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.file;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;

/**
 * @deprecated moved to com.intellij.psi.impl.file.SourceRootIconProvider.DirectoryProvider
 */
@Deprecated
public class DirectoryIconProvider extends SourceRootIconProvider.DirectoryProvider {
  public static Icon getDirectoryIcon(VirtualFile vFile, Project project) {
    return SourceRootIconProvider.getDirectoryIcon(vFile, project);
  }
}
