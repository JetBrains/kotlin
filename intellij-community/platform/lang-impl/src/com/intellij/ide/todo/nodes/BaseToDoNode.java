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

package com.intellij.ide.todo.nodes;

import com.intellij.ide.todo.ToDoSettings;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.todo.TodoTreeStructure;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class BaseToDoNode<Value> extends AbstractTreeNode<Value> {
  protected final ToDoSettings myToDoSettings;
  protected final TodoTreeBuilder myBuilder;

  protected BaseToDoNode(Project project, @NotNull Value value, TodoTreeBuilder builder) {
    super(project, value);
    myBuilder = builder;
    myToDoSettings = myBuilder.getTodoTreeStructure();
  }

  public boolean contains(Object element) {
    return false;
  }

  protected TodoTreeStructure getTreeStructure() {
    return myBuilder.getTodoTreeStructure();
  }

  public abstract int getFileCount(Value val);

  public abstract int getTodoItemCount(Value val);
}
