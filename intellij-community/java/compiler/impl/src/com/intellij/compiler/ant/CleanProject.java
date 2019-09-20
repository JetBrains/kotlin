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
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.artifacts.ArtifactsGenerator;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class CleanProject extends Generator {
  private final Target myTarget;

  public CleanProject(Project project, @NotNull GenerationOptions genOptions, @NotNull ArtifactsGenerator artifactsGenerator) {
    List<String> dependencies = new ArrayList<>();
    final ModuleChunk[] chunks = genOptions.getModuleChunks();
    for (ModuleChunk chunk : chunks) {
      dependencies.add(BuildProperties.getModuleCleanTargetName(chunk.getName()));
    }
    dependencies.addAll(artifactsGenerator.getCleanTargetNames());
    for (ChunkBuildExtension extension : ChunkBuildExtension.EP_NAME.getExtensions()) {
      dependencies.addAll(extension.getCleanTargetNames(project, genOptions));
    }
    myTarget = new Target(BuildProperties.TARGET_CLEAN, StringUtil.join(dependencies, ", "),
                          CompilerBundle.message("generated.ant.build.clean.all.task.comment"), null);
  }

  @Override
  public void generate(PrintWriter out) throws IOException {
    myTarget.generate(out);
  }
}
