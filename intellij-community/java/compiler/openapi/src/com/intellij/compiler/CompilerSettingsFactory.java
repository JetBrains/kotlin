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
package com.intellij.compiler;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

/**
 * Please use {@link com.intellij.openapi.options.ConfigurableEP#parentId} to put your configurable under Compiler Settings
 * @author Eugene Zhuravlev
 */
@Deprecated
public interface CompilerSettingsFactory {
  ExtensionPointName<CompilerSettingsFactory> EP_NAME = ExtensionPointName.create("com.intellij.compilerSettingsFactory");

  Configurable create(Project project);
}
