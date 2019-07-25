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

import com.intellij.compiler.ant.taskdefs.Import;
import com.intellij.openapi.project.Project;

import java.io.File;

/**
 * @author Eugene Zhuravlev
 */
public class MultipleFileProjectBuild extends ProjectBuild{
  public MultipleFileProjectBuild(Project project, GenerationOptions genOptions) {
    super(project, genOptions);
  }

  @Override
  protected Generator createModuleBuildGenerator(ModuleChunk chunk, GenerationOptions genOptions) {
    //noinspection HardCodedStringLiteral
    final String chunkBuildFile = BuildProperties.getModuleChunkBaseDir(chunk).getPath() + File.separator + BuildProperties.getModuleChunkBuildFileName(chunk) + ".xml";
    final File projectBaseDir = BuildProperties.getProjectBaseDir(myProject);
    final String pathToFile = GenerationUtils.toRelativePath(
      chunkBuildFile, projectBaseDir, BuildProperties.getProjectBaseDirProperty(), genOptions);
    return new Import(pathToFile);
  }

}
