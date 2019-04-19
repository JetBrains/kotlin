/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.impl;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.StandardTargetWeights;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.ide.actions.ShowFilePathAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

/**
 * @author Roman.Chernyatchik
 */
public class ProjectViewSelectInExplorerTarget implements DumbAware, SelectInTarget {
  @Override
  public boolean canSelect(SelectInContext context) {
    VirtualFile file = ShowFilePathAction.findLocalFile(context.getVirtualFile());
    return file != null;
  }

  @Override
  public void selectIn(SelectInContext context, boolean requestFocus) {
    VirtualFile file = ShowFilePathAction.findLocalFile(context.getVirtualFile());
    if (file != null) {
      ShowFilePathAction.openFile(new File(file.getPresentableUrl()));
    }
  }

  @Override
  public String toString() {
    return RevealFileAction.getActionName();
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.OS_FILE_MANAGER;
  }
}
