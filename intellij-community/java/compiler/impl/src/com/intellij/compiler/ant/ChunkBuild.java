/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.compiler.ant.taskdefs.Path;
import com.intellij.compiler.ant.taskdefs.Property;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.File;

/**
 * @author Eugene Zhuravlev
 */
public class ChunkBuild extends CompositeGenerator{

  public ChunkBuild(Project project, ModuleChunk chunk, GenerationOptions genOptions) {
    final File chunkBaseDir = chunk.getBaseDir();
    if (genOptions.forceTargetJdk) {
      if (chunk.isJdkInherited()) {
        add(new Property(BuildProperties.getModuleChunkJdkHomeProperty(chunk.getName()), BuildProperties.propertyRef(BuildProperties.PROPERTY_PROJECT_JDK_HOME)));
        add(new Property(BuildProperties.getModuleChunkJdkBinProperty(chunk.getName()), BuildProperties.propertyRef(BuildProperties.PROPERTY_PROJECT_JDK_BIN)));
        add(new Property(BuildProperties.getModuleChunkJdkClasspathProperty(chunk.getName()), BuildProperties.propertyRef(BuildProperties.PROPERTY_PROJECT_JDK_CLASSPATH)));
      }
      else {
        final Sdk jdk = chunk.getJdk();
        add(new Property(BuildProperties.getModuleChunkJdkHomeProperty(chunk.getName()), jdk != null? BuildProperties.propertyRef(BuildProperties.getJdkHomeProperty(jdk.getName())): ""));
        add(new Property(BuildProperties.getModuleChunkJdkBinProperty(chunk.getName()), jdk != null? BuildProperties.propertyRef(BuildProperties.getJdkBinProperty(jdk.getName())): ""));
        add(new Property(BuildProperties.getModuleChunkJdkClasspathProperty(chunk.getName()), jdk != null? BuildProperties.getJdkPathId(jdk.getName()) : ""));
      }
    }

    final StringBuilder compileArgs = new StringBuilder();
    compileArgs.append(chunk.getChunkSpecificCompileOptions());
    if (compileArgs.length() > 0) {
      compileArgs.append(" ");
    }
    compileArgs.append(BuildProperties.propertyRef(BuildProperties.PROPERTY_COMPILER_ADDITIONAL_ARGS));
    add(new Property(BuildProperties.getModuleChunkCompilerArgsProperty(chunk.getName()), compileArgs.toString()), 1);

    final String outputPathUrl = chunk.getOutputDirUrl();
    String location = outputPathUrl != null?
                      GenerationUtils.toRelativePath(VirtualFileManager.extractPath(outputPathUrl), chunkBaseDir, BuildProperties.getModuleChunkBasedirProperty(chunk), genOptions) :
                      CompilerBundle.message("value.undefined");
    add(new Property(BuildProperties.getOutputPathProperty(chunk.getName()), location), 1);

    final String testOutputPathUrl = chunk.getTestsOutputDirUrl();
    if (testOutputPathUrl != null) {
      location = GenerationUtils.toRelativePath(VirtualFileManager.extractPath(testOutputPathUrl), chunkBaseDir, BuildProperties.getModuleChunkBasedirProperty(chunk), genOptions);
    }
    add(new Property(BuildProperties.getOutputPathForTestsProperty(chunk.getName()), location));

    add(createBootclasspath(chunk), 1);
    add(new ModuleChunkClasspath(chunk, genOptions, false, false), 1);
    add(new ModuleChunkClasspath(chunk, genOptions, true, false), 1);
    add(new ModuleChunkClasspath(chunk, genOptions, false, true), 1);
    add(new ModuleChunkClasspath(chunk, genOptions, true, true), 1);

    final ModuleChunkSourcePath moduleSources = new ModuleChunkSourcePath(project, chunk, genOptions);
    add(moduleSources, 1);
    add(new CompileModuleChunkTarget(project, chunk, moduleSources.getSourceRoots(), moduleSources.getTestSourceRoots(), chunkBaseDir, genOptions), 1);
    add(new CleanModule(chunk), 1);

    ChunkBuildExtension.process(this, chunk, genOptions);
  }

  private static Generator createBootclasspath(ModuleChunk chunk) {
    final Path bootclasspath = new Path(BuildProperties.getBootClasspathProperty(chunk.getName()));
    bootclasspath.add(new Comment(CompilerBundle.message("generated.ant.build.bootclasspath.comment")));
    return bootclasspath;
  }


}
