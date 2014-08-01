/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.FileProcessor;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.compiler.JpsCompilerExcludes;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KotlinSourceFileCollector {
    // For incremental compilation
    public static MultiMap<ModuleBuildTarget, File> getDirtySourceFiles(DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder)
            throws IOException
    {
        final MultiMap<ModuleBuildTarget, File> result = new MultiMap<ModuleBuildTarget, File>();

        dirtyFilesHolder.processDirtyFiles(new FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>() {
            @Override
            public boolean apply(ModuleBuildTarget target, File file, JavaSourceRootDescriptor root) throws IOException {
                if (isKotlinSourceFile(file)) {
                    result.putValue(target, file);
                }
                return true;
            }
        });
        return result;
    }

    @NotNull
    public static List<String> getRemovedKotlinFiles(
            @NotNull DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget> dirtyFilesHolder,
            @NotNull ModuleBuildTarget target
    ) throws IOException {
        return ContainerUtil.filter(dirtyFilesHolder.getRemovedFiles(target), new Condition<String>() {
            @Override
            public boolean value(String s) {
                return FileUtilRt.extensionEquals(s, "kt");
            }
        });
    }

    @NotNull
    public static List<File> getAllKotlinSourceFiles(@NotNull ModuleBuildTarget target) {
        final List<File> moduleExcludes = ContainerUtil.map(target.getModule().getExcludeRootsList().getUrls(), new Function<String, File>() {
            @Override
            public File fun(String url) {
                return JpsPathUtil.urlToFile(url);
            }
        });

        final JpsCompilerExcludes compilerExcludes =
                JpsJavaExtensionService.getInstance().getOrCreateCompilerConfiguration(target.getModule().getProject()).getCompilerExcludes();

        final List<File> result = ContainerUtil.newArrayList();
        for (JpsModuleSourceRoot sourceRoot : getRelevantSourceRoots(target)) {
            FileUtil.processFilesRecursively(
                    sourceRoot.getFile(),
                    new Processor<File>() {
                        @Override
                        public boolean process(File file) {
                            if (compilerExcludes.isExcluded(file)) return true;

                            if (file.isFile() && isKotlinSourceFile(file)) {
                                result.add(file);
                            }
                            return true;
                        }
                    },
                    new Processor<File>() {
                        @Override
                        public boolean process(final File dir) {
                            return ContainerUtil.find(moduleExcludes, new Condition<File>() {
                                @Override
                                public boolean value(File exclude) {
                                    return FileUtil.filesEqual(exclude, dir);
                                }
                            }) == null;
                        }
                    });
        }
        return result;
    }

    private static Iterable<JpsModuleSourceRoot> getRelevantSourceRoots(ModuleBuildTarget target) {
        JavaSourceRootType sourceRootType = target.isTests() ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE;

        //noinspection unchecked
        return (Iterable) target.getModule().getSourceRoots(sourceRootType);
    }

    private static boolean isKotlinSourceFile(File file) {
        return FileUtilRt.extensionEquals(file.getName(), "kt");
    }

    private KotlinSourceFileCollector() {}
}
