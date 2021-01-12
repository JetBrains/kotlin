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
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface GradleTestTasksProvider {
  ExtensionPointName<GradleTestTasksProvider> EP_NAME = ExtensionPointName.create("org.jetbrains.plugins.gradle.testTasksProvider");

  @NotNull
  List<String> getTasks(@NotNull Module module);

  @NotNull
  default List<String> getTasks(@NotNull Module module, @NotNull VirtualFile source) {
    return getTasks(module);
  }
}
