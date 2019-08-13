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

import com.intellij.ide.todo.nodes.TodoItemNode;
import com.intellij.openapi.util.TextRange;

import java.util.Comparator;

/**
 * @author Vladimir Kondratyev
 */
public final class SmartTodoItemPointerComparator implements Comparator{
  public static final SmartTodoItemPointerComparator ourInstance=new SmartTodoItemPointerComparator();

  private SmartTodoItemPointerComparator(){}

  @Override
  public int compare(Object obj1,Object obj2){
    TextRange range1=((TodoItemNode)obj1).getValue().getTodoItem().getTextRange();
    TextRange range2=((TodoItemNode)obj2).getValue().getTodoItem().getTextRange();
    return range1.getStartOffset()-range2.getStartOffset();
  }
}
