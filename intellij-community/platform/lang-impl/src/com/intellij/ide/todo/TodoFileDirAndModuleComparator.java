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

package com.intellij.ide.todo;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.todo.nodes.ModuleToDoNode;
import com.intellij.ide.util.treeView.NodeDescriptor;

import java.util.Comparator;

public final class TodoFileDirAndModuleComparator implements Comparator<NodeDescriptor>{
  public static final TodoFileDirAndModuleComparator INSTANCE =new TodoFileDirAndModuleComparator();

  private TodoFileDirAndModuleComparator(){}

  @Override
  public int compare(NodeDescriptor obj1, NodeDescriptor obj2){
    final int weight1 = obj1.getWeight();
    final int weight2 = obj2.getWeight();
    if (weight1 != weight2) return weight1 - weight2;
    if (obj1 instanceof ProjectViewNode && obj2 instanceof ProjectViewNode) {
      return ((ProjectViewNode)obj1).getTitle().compareToIgnoreCase(((ProjectViewNode)obj2).getTitle());
    }
    if (obj1 instanceof ModuleToDoNode && obj2 instanceof ModuleToDoNode){
      return ((ModuleToDoNode)obj1).getValue().getName().compareToIgnoreCase(((ModuleToDoNode)obj2).getValue().getName());
    } else if(obj1 instanceof ModuleToDoNode) {
      return -1;
    } else if(obj2 instanceof ModuleToDoNode) {
      return 1;
    } else{
      throw new IllegalArgumentException(obj1.getClass().getName()+","+obj2.getClass().getName());
    }
  }
}
