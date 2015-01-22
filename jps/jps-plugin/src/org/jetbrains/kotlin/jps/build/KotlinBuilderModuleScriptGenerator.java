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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.builders.logging.ProjectBuilderLogger;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.model.java.JpsAnnotationRootType;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsSdkDependency;
import org.jetbrains.kotlin.config.IncrementalCompilation;
import org.jetbrains.kotlin.modules.KotlinModuleDescriptionBuilder;
import org.jetbrains.kotlin.modules.KotlinModuleDescriptionBuilderFactory;
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.kotlin.jps.build.JpsUtils.getAllDependencies;
import static org.jetbrains.kotlin.modules.KotlinModuleDescriptionBuilder.DependencyProcessor;
import static org.jetbrains.kotlin.modules.KotlinModuleDescriptionBuilder.DependencyProvider;

public class KotlinBuilderModuleScriptGenerator {

    public static final KotlinModuleDescriptionBuilderFactory FACTORY = KotlinModuleXmlBuilderFactory.INSTANCE;

    @Nullable
    public static File generateModuleDescription(
            CompileContext context,
            ModuleChunk chunk,
            MultiMap<ModuleBuildTarget, File> sourceFiles, // ignored for non-incremental compilation
            boolean hasRemovedFiles
    ) throws IOException, ProjectBuildException {
        KotlinModuleDescriptionBuilder builder = FACTORY.create();

        boolean noSources = true;

        Set<File> outputDirs = new HashSet<File>();
        for (ModuleBuildTarget target : chunk.getTargets()) {
            outputDirs.add(getOutputDirSafe(target));
        }
        ProjectBuilderLogger logger = context.getLoggingManager().getProjectBuilderLogger();
        for (ModuleBuildTarget target : chunk.getTargets()) {
            File outputDir = getOutputDirSafe(target);

            List<File> moduleSources = new ArrayList<File>(
                    IncrementalCompilation.ENABLED
                    ? sourceFiles.get(target)
                    : KotlinSourceFileCollector.getAllKotlinSourceFiles(target));

            if (moduleSources.size() > 0 || hasRemovedFiles) {
                noSources = false;

                if (logger.isEnabled()) {
                    logger.logCompiledFiles(moduleSources, KotlinBuilder.KOTLIN_BUILDER_NAME, "Compiling files:");
                }
            }

            builder.addModule(
                    target.getId(),
                    outputDir.getAbsolutePath(),
                    getKotlinModuleDependencies(target),
                    moduleSources,
                    findSourceRoots(context, target),
                    target.isTests(),
                    // this excludes the output directories from the class path, to be removed for true incremental compilation
                    outputDirs
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

    private static DependencyProvider getKotlinModuleDependencies(final ModuleBuildTarget target) {
        return new DependencyProvider() {
            @Override
            public void processClassPath(@NotNull DependencyProcessor processor) {
                processor.processClassPathSection("Classpath", findClassPathRoots(target));
                processor.processAnnotationRoots(findAnnotationRoots(target));
            }
        };
    }

    @NotNull
    private static Collection<File> findClassPathRoots(@NotNull ModuleBuildTarget target) {
        return getAllDependencies(target).classes().getRoots();
    }

    @NotNull
    private static List<File> findSourceRoots(@NotNull CompileContext context, @NotNull ModuleBuildTarget target) {
        List<JavaSourceRootDescriptor> roots = context.getProjectDescriptor().getBuildRootIndex().getTargetRoots(target, context);
        List<File> result = ContainerUtil.newArrayList();
        for (JavaSourceRootDescriptor root : roots) {
            File file = root.getRootFile();
            if (file.exists()) {
                result.add(file);
            }
        }
        return result;
    }

    @NotNull
    private static List<File> findAnnotationRoots(@NotNull ModuleBuildTarget target) {
        LinkedHashSet<File> annotationRootFiles = new LinkedHashSet<File>();

        JpsModule module = target.getModule();
        JpsSdk sdk = module.getSdk(getSdkType(module));
        if (sdk != null) {
            annotationRootFiles.addAll(sdk.getParent().getFiles(JpsAnnotationRootType.INSTANCE));
        }

        for (JpsLibrary library : getAllDependencies(target).getLibraries()) {
            annotationRootFiles.addAll(library.getFiles(JpsAnnotationRootType.INSTANCE));
        }

        // JDK is stored locally on user's machine, so its configuration, including external annotation paths
        // is not available on TeamCity. When running on TeamCity, one has to provide extra path to JDK annotations
        String extraAnnotationsPaths = System.getProperty("jps.kotlin.extra.annotation.paths");
        if (extraAnnotationsPaths != null) {
            String[] paths = extraAnnotationsPaths.split(";");
            for (String path : paths) {
                annotationRootFiles.add(new File(path));
            }
        }

        return KotlinPackage.toList(annotationRootFiles);
    }

    @NotNull
    private static JpsSdkType getSdkType(@NotNull JpsModule module) {
        for (JpsDependencyElement dependency : module.getDependenciesList().getDependencies()) {
            if (dependency instanceof JpsSdkDependency) {
                return ((JpsSdkDependency) dependency).getSdkType();
            }
        }
        return JpsJavaSdkType.INSTANCE;
    }

    private KotlinBuilderModuleScriptGenerator() {}
}
