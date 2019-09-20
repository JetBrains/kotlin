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

/*
 * @author max
 */
package com.intellij.ide.impl;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import com.intellij.ide.projectView.impl.ProjectTreeStructure;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.StructureViewModuleNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ModuleStructurePane extends ProjectViewPane {
  @NotNull
  private final Module myModule;

  public ModuleStructurePane(@NotNull Module module) {
    super(module.getProject());
    myModule = module;
  }

  @NotNull
  @Override
  protected ProjectAbstractTreeStructureBase createStructure() {
    return new ProjectTreeStructure(myProject, ID){
      @Override
      protected AbstractTreeNode createRoot(@NotNull final Project project, @NotNull ViewSettings settings) {
        return new StructureViewModuleNode(project, myModule, settings);
      }
    };
  }
}
