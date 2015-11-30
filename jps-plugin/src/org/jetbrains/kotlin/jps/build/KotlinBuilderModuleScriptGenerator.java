/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.BuildTargetType;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.builders.logging.ProjectBuilderLogger;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.kotlin.config.IncrementalCompilation;
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.kotlin.jps.build.JpsUtils.getAllDependencies;

public class KotlinBuilderModuleScriptGenerator {

    @Nullable
    public static File generateModuleDescription(
            CompileContext context,
            ModuleChunk chunk,
            MultiMap<ModuleBuildTarget, File> sourceFiles, // ignored for non-incremental compilation
            boolean hasRemovedFiles
    ) throws IOException, ProjectBuildException {
        KotlinModuleXmlBuilder builder = new KotlinModuleXmlBuilder();

        boolean noSources = true;

        Set<File> outputDirs = new HashSet<File>();
        for (ModuleBuildTarget target : chunk.getTargets()) {
            outputDirs.add(getOutputDirSafe(target));
        }
        ProjectBuilderLogger logger = context.getLoggingManager().getProjectBuilderLogger();
        for (ModuleBuildTarget target : chunk.getTargets()) {
            File outputDir = getOutputDirSafe(target);
            List<File> friendDirs = new ArrayList<File>();
            File friendDir = getFriendDirSafe(target);
            if (friendDir != null) {
                friendDirs.add(friendDir);
            }

            List<File> moduleSources = new ArrayList<File>(
                    IncrementalCompilation.isEnabled()
                    ? sourceFiles.get(target)
                    : KotlinSourceFileCollector.getAllKotlinSourceFiles(target));

            if (moduleSources.size() > 0 || hasRemovedFiles) {
                noSources = false;

                if (logger.isEnabled()) {
                    logger.logCompiledFiles(moduleSources, KotlinBuilder.KOTLIN_BUILDER_NAME, "Compiling files:");
                }
            }

            BuildTargetType<?> targetType = target.getTargetType();
            assert targetType instanceof JavaModuleBuildTargetType;
            builder.addModule(
                    target.getId(),
                    outputDir.getAbsolutePath(),
                    moduleSources,
                    findSourceRoots(context, target),
                    findClassPathRoots(target),
                    (JavaModuleBuildTargetType) targetType,
                    // this excludes the output directories from the class path, to be removed for true incremental compilation
                    outputDirs,
                    friendDirs
            );
        }

        if (noSources) return null;

        File scriptFile = File.createTempFile("kjps", StringUtil.sanitizeJavaIdentifier(chunk.getName()) + ".script.xml");

        FileUtil.writeToFile(scriptFile, builder.asText().toString());

        return scriptFile;
    }

    @NotNull
    public static File getOutputDirSafe(@NotNull ModuleBuildTarget target) throws ProjectBuildException {
        File outputDir = target.getOutputDir();
        if (outputDir == null) {
            throw new ProjectBuildException("No output directory found for " + target);
        }
        return outputDir;
    }

    @Nullable
    public static File getFriendDirSafe(@NotNull ModuleBuildTarget target) throws ProjectBuildException {
        if (!target.isTests()) return null;

        File outputDirForProduction = JpsJavaExtensionService.getInstance().getOutputDirectory(target.getModule(), false);
        if (outputDirForProduction == null) {
            throw new ProjectBuildException("No output production directory found for " + target);
        }
        return outputDirForProduction;
    }

    @NotNull
    private static Collection<File> findClassPathRoots(@NotNull ModuleBuildTarget target) {
        return ContainerUtil.filter(getAllDependencies(target).classes().getRoots(), new Condition<File>() {
            @Override
            public boolean value(File file) {
                if (!file.exists()) {
                    String extension = FilesKt.getExtension(file);

                    // Don't filter out files, we want to report warnings about absence through the common place
                    if (!(extension.equals("class") || extension.equals("jar"))) {
                        return false;
                    }
                }

                return true;
            }
        });
    }

    @NotNull
    private static List<JvmSourceRoot> findSourceRoots(@NotNull CompileContext context, @NotNull ModuleBuildTarget target) {
        List<JavaSourceRootDescriptor> roots = context.getProjectDescriptor().getBuildRootIndex().getTargetRoots(target, context);
        List<JvmSourceRoot> result = ContainerUtil.newArrayList();
        for (JavaSourceRootDescriptor root : roots) {
            File file = root.getRootFile();
            String prefix = root.getPackagePrefix();
            if (file.exists()) {
                result.add(new JvmSourceRoot(file, prefix.isEmpty() ? null : prefix));
            }
        }
        return result;
    }

    private KotlinBuilderModuleScriptGenerator() {}
}
