/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.jps.build;

import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.FileProcessor;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.*;

import java.io.File;
import java.io.IOException;

public class KotlinBuilder extends ModuleLevelBuilder {
    protected KotlinBuilder() {
        super(BuilderCategory.SOURCE_PROCESSOR);
    }

    @Override
    public ExitCode build(
            CompileContext context, ModuleChunk chunk, DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder
    ) throws ProjectBuildException {
        try {
            dirtyFilesHolder.processDirtyFiles(new FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>() {
                @Override
                public boolean apply(ModuleBuildTarget target, File file, JavaSourceRootDescriptor root) throws IOException {
                    if (file.getName().endsWith(".kt") || file.getName().endsWith(".java")) {

                    }
                    return true;
                }
            });
        }
        catch (IOException e) {
            throw new ProjectBuildException(e);
        }
        //context.processMessage(new CompilerMessage());
        //JpsJavaExtensionService.dependencies().recursively().productionOnly().classes().getRoots()
        return ExitCode.OK;
    }

    @Override
    public String getName() {
        return "KotlinBuilder";
    }

    @Override
    public String getDescription() {
        return "KotlinBuilder";
    }
}
