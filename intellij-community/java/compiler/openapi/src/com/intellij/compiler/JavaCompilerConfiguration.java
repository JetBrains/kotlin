// Copyright 2000-2017 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.intellij.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCompilerConfigurationProxy;

import java.util.List;

public class JavaCompilerConfiguration extends JavaCompilerConfigurationProxy {
  @Override
  public List<String> getAdditionalOptionsImpl(Project project, Module module) {
    return CompilerConfiguration.getInstance(project).getAdditionalOptions(module);
  }

  @Override
  public void setAdditionalOptionsImpl(Project project, Module module, List<String> options) {
    CompilerConfiguration.getInstance(project).setAdditionalOptions(module, options);
  }
}
