/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.todo;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Vladimir Kondratyev
 */
public class CurrentFileTodosTreeBuilder extends TodoTreeBuilder {
  public CurrentFileTodosTreeBuilder(JTree tree, Project project){
    super(tree, project);
  }

  @Override
  @NotNull
  protected TodoTreeStructure createTreeStructure(){
    return new CurrentFileTodosTreeStructure(myProject);
  }

  @Override
  void rebuildCache(){
    Set<VirtualFile> files = new HashSet<>();
    CurrentFileTodosTreeStructure treeStructure=(CurrentFileTodosTreeStructure)getTodoTreeStructure();
    PsiFile psiFile=treeStructure.getFile();
    if(treeStructure.accept(psiFile)){
      files.add(psiFile.getVirtualFile());
    }
    super.rebuildCache(files);
  }

  /**
   * @see com.intellij.ide.todo.CurrentFileTodosTreeStructure#setFile
   */
  public void setFile(PsiFile file){
    CurrentFileTodosTreeStructure treeStructure=(CurrentFileTodosTreeStructure)getTodoTreeStructure();
    treeStructure.setFile(file);
    rebuildCache();
    updateTree();
  }
}
