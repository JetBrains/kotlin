/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

public class AllTodosTreeBuilder extends TodoTreeBuilder {
  /**
   * @deprecated To remove in 2020.1
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public AllTodosTreeBuilder(JTree tree, DefaultTreeModel treeModel, Project project) {
    this(tree, project);
  }

  public AllTodosTreeBuilder(JTree tree, Project project) {
    super(tree, project);
  }

  @Override
  @NotNull
  protected TodoTreeStructure createTreeStructure() {
    return new AllTodosTreeStructure(myProject);
  }
}
