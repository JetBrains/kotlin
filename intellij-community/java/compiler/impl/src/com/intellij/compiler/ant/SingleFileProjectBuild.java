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

import com.intellij.compiler.ant.taskdefs.Dirname;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;

/**
 * @author Eugene Zhuravlev
 */
public class SingleFileProjectBuild extends ProjectBuild {
  public SingleFileProjectBuild(Project project, GenerationOptions genOptions) {
    super(project, genOptions);
  }

  @Override
  protected Generator createModuleBuildGenerator(ModuleChunk chunk, GenerationOptions genOptions) {
    final CompositeGenerator gen = new CompositeGenerator();
    gen.add(new Comment(CompilerBundle.message("generated.ant.build.building.concrete.module.section.title", chunk.getName())));
    gen.add(new Dirname(BuildProperties.getModuleChunkBasedirProperty(chunk), BuildProperties.propertyRef("ant.file")), 1);
    gen.add(new ChunkBuild(myProject, chunk, genOptions), 1);
    return gen;
  }

}
