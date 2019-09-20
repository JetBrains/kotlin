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

import com.intellij.compiler.ant.taskdefs.AntProject;
import com.intellij.compiler.ant.taskdefs.Dirname;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Eugene Zhuravlev
 */
public class ModuleChunkAntProject extends Generator{
  private final AntProject myAntProject;

  public ModuleChunkAntProject(Project project, ModuleChunk moduleChunk, GenerationOptions genOptions) {
    myAntProject = new AntProject(BuildProperties.getModuleChunkBuildFileName(moduleChunk), BuildProperties.getCompileTargetName(moduleChunk.getName()));
    myAntProject.add(new Dirname(BuildProperties.getModuleChunkBasedirProperty(moduleChunk), BuildProperties.propertyRef("ant.file." + BuildProperties.getModuleChunkBuildFileName(moduleChunk))));
    myAntProject.add(new ChunkBuild(project, moduleChunk, genOptions));

  }

  @Override
  public void generate(PrintWriter out) throws IOException {
    writeXmlHeader(out);
    myAntProject.generate(out);
  }


}
