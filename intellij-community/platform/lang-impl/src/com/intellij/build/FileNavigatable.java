// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class FileNavigatable implements Navigatable {
  private final Project myProject;
  private final NullableLazyValue<Navigatable> myValue;
  private final FilePosition myFilePosition;

  public FileNavigatable(Project project, FilePosition filePosition) {
    myProject = project;
    myFilePosition = filePosition;
    myValue = new NullableLazyValue<Navigatable>() {
      @Nullable
      @Override
      protected Navigatable compute() {
        return getDescriptor();
      }
    };
  }

  @Override
  public void navigate(boolean requestFocus) {
    Navigatable descriptor = myValue.getValue();
    if (descriptor != null) {
      descriptor.navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    Navigatable descriptor = myValue.getValue();
    if (descriptor != null) {
      return descriptor.canNavigate();
    }
    return false;
  }

  @Override
  public boolean canNavigateToSource() {
    Navigatable descriptor = myValue.getValue();
    if (descriptor != null) {
      return descriptor.canNavigateToSource();
    }
    return false;
  }

  @Nullable
  private Navigatable getDescriptor() {
    Navigatable descriptor = null;
    VirtualFile file = VfsUtil.findFileByIoFile(myFilePosition.getFile(), false);
    if (file != null) {
      descriptor = new OpenFileDescriptor(myProject, file, myFilePosition.getStartLine(), myFilePosition.getStartColumn());
    }
    return descriptor;
  }
}
