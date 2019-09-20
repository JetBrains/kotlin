/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.compiler.options;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Compiler settings page (Project Settings | Compiler) offers number of controls. However, there is a possible case that
 * some of them should be 'frozen' at particular environment (e.g. there is no point in disable external compiler
 * in case of AndroidStudio).
 * <p/>
 * This interface defines api which allows extensions to control compiler options availability.
 * 
 * @author Denis Zhdanov
 */
public interface CompilerOptionsFilter {

  ExtensionPointName<CompilerOptionsFilter> EP_NAME = ExtensionPointName.create("com.intellij.compiler.optionsManager");

  enum Setting {
    RESOURCE_PATTERNS, CLEAR_OUTPUT_DIR_ON_REBUILD, ADD_NOT_NULL_ASSERTIONS, AUTO_SHOW_FIRST_ERROR_IN_EDITOR,
    EXTERNAL_BUILD, AUTO_MAKE, PARALLEL_COMPILATION, REBUILD_MODULE_ON_DEPENDENCY_CHANGE, HEAP_SIZE, COMPILER_VM_OPTIONS, DISPLAY_NOTIFICATION_POPUP
  }

  boolean isAvailable(@NotNull Setting setting, @NotNull Project project);
}
