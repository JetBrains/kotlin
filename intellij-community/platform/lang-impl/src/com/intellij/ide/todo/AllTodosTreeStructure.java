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

import com.intellij.ide.todo.nodes.ToDoRootNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

/**
 * @author Vladimir Kondratyev
 */
public class AllTodosTreeStructure extends TodoTreeStructure {
  public AllTodosTreeStructure(final Project project) {
    super(project);
  }

  @Override
  public boolean accept(final PsiFile psiFile) {
    final boolean
            accept = psiFile.isValid() &&
            (
            myTodoFilter != null && myTodoFilter.accept(mySearchHelper, psiFile) ||
            (myTodoFilter == null && mySearchHelper.getTodoItemsCount(psiFile) > 0)
            );
    return accept;
  }

  @Override
  public boolean getIsPackagesShown() {
    return myArePackagesShown;
  }

  @Override
  Object getFirstSelectableElement() {
    return ((ToDoRootNode)myRootElement).getSummaryNode();
  }

  @Override
  protected AbstractTreeNode createRootElement() {
    return new ToDoRootNode(myProject, new Object(),
                            myBuilder, mySummaryElement);
  }
}