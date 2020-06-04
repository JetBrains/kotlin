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

import com.intellij.ide.todo.nodes.SingleFileToDoNode;
import com.intellij.ide.todo.nodes.ToDoRootNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public final class CurrentFileTodosTreeStructure extends TodoTreeStructure{
  /**
   * Current {@code VirtualFile} for which the structure is built. If {@code myFile} is {@code null}
   * then the structure is empty (contains only root node).
   */
  private PsiFile myFile;

  public CurrentFileTodosTreeStructure(Project project){
    super(project);
  }

  @Override
  protected void validateCache(){
    super.validateCache();
    if(myFile!=null && !myFile.isValid()){
      VirtualFile vFile=myFile.getVirtualFile();
      if(vFile.isValid()){
        myFile=PsiManager.getInstance(myProject).findFile(vFile);
      }else{
        myFile=null;
      }
    }
  }

  PsiFile getFile(){
    return myFile;
  }

  /**
   * Sets {@code file} for which the structure is built. Alter this method is invoked caches should
   * be validated.
   */
  public void setFile(PsiFile file){
    myFile=file;
    myRootElement = createRootElement();
  }

  @Override
  public boolean accept(PsiFile psiFile){
    if(myFile==null||!myFile.equals(psiFile)||!myFile.isValid()){
      return false;
    }
    return (myTodoFilter!=null&&myTodoFilter.accept(mySearchHelper,psiFile))||
      (myTodoFilter==null&&mySearchHelper.getTodoItemsCount(psiFile)>0);
  }

  @Override
  boolean isAutoExpandNode(NodeDescriptor descriptor){
    Object element=descriptor.getElement();
    if (element instanceof AbstractTreeNode) {
      element = ((AbstractTreeNode)element).getValue();
    }
    if(element==myFile){
      return true;
    }else{
      return element == getRootElement() || element == mySummaryElement;
    }
  }

  @Override
  Object getFirstSelectableElement(){
    if (myRootElement instanceof SingleFileToDoNode){
      return ((SingleFileToDoNode)myRootElement).getFileNode();
    } else {
      return null;
    }
  }

  @Override
  public boolean getIsPackagesShown() {
    return myArePackagesShown;
  }

  @Override
  protected AbstractTreeNode createRootElement() {
    if  (!accept(myFile)) {
      return new ToDoRootNode(myProject, new Object(), myBuilder, mySummaryElement);
    } else {
      return new SingleFileToDoNode(myProject, myFile, myBuilder);
    }

  }
}