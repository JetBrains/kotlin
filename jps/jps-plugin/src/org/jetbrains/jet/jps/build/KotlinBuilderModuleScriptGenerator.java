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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.runner.KotlinModuleDescriptionGenerator;
import org.jetbrains.jet.compiler.runner.KotlinModuleScriptGenerator;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.model.java.JpsAnnotationRootType;
import org.jetbrains.jps.model.java.JpsJavaClasspathKind;
import org.jetbrains.jps.model.java.JpsJavaDependenciesEnumerator;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryRoot;
import org.jetbrains.jps.model.module.*;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.compiler.runner.KotlinModuleDescriptionGenerator.DependencyProvider;

public class KotlinBuilderModuleScriptGenerator {
    public static File generateModuleScript(CompileContext context, ModuleBuildTarget target, List<File> sourceFiles)
            throws IOException
    {
        CharSequence moduleScriptText = KotlinModuleScriptGenerator.INSTANCE.generateModuleScript(
                target.getId(),
                getKotlinModuleDependencies(context, target),
                sourceFiles,
                target.isTests(),
                // this excludes the output directory from the class path, to be removed for true incremental compilation
                Collections.singleton(target.getOutputDir())
        );

        File scriptFile = new File(target.getOutputDir(), "script.kts");

        writeScriptToFile(context, moduleScriptText, scriptFile);

        return scriptFile;
    }

    private static DependencyProvider getKotlinModuleDependencies(final CompileContext context, final ModuleBuildTarget target) {
        return new DependencyProvider() {
            @Override
            public void processClassPath(@NotNull KotlinModuleDescriptionGenerator.DependencyProcessor processor) {
                processor.processClassPathSection("Classpath", findClassPathRoots(target));
                processor.processClassPathSection("Java Source Roots", findSourceRoots(context, target));
                processor.processAnnotationRoots(findAnnotationRoots(target));
            }
        };
    }

    private static void writeScriptToFile(CompileContext context, CharSequence moduleScriptText, File scriptFile) throws IOException {
        FileUtil.writeToFile(scriptFile, moduleScriptText.toString());
        context.processMessage(new CompilerMessage(
                "Kotlin",
                BuildMessage.Kind.INFO,
                "Created script file: " + scriptFile
        ));
    }

    @NotNull
    private static Collection<File> findClassPathRoots(@NotNull ModuleBuildTarget target) {
        JpsModule module = target.getModule();
        JpsJavaDependenciesEnumerator dependencies = JpsJavaExtensionService.dependencies(module)
                .includedIn(JpsJavaClasspathKind.compile(target.isTests()));

        return dependencies.classes().getRoots();
    }

    @NotNull
    private static Collection<File> findSourceRoots(@NotNull CompileContext context, @NotNull ModuleBuildTarget target) {
        List<JavaSourceRootDescriptor> roots = context.getProjectDescriptor().getBuildRootIndex().getTargetRoots(target, context);
        Collection<File> result = ContainerUtil.newArrayList();
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
        JpsModule module = target.getModule();
        List<JpsDependencyElement> dependencies = module.getDependenciesList().getDependencies();

        List<File> annotationRootFiles = ContainerUtil.newArrayList();
        for (JpsDependencyElement dependencyElement : dependencies) {
            JpsLibrary library = getLibrary(dependencyElement);
            if (library == null) continue;

            List<JpsLibraryRoot> annotationRoots = library.getRoots(JpsAnnotationRootType.INSTANCE);
            for (JpsLibraryRoot root : annotationRoots) {
                File file = new File(JpsPathUtil.urlToPath(root.getUrl()));

                annotationRootFiles.add(file);
            }
        }

        return annotationRootFiles;
    }

    @Nullable
    private static JpsLibrary getLibrary(@NotNull JpsDependencyElement dependencyElement) {
        if (dependencyElement instanceof JpsSdkDependency) {
            JpsSdkDependency sdkDependency = (JpsSdkDependency) dependencyElement;
            return sdkDependency.resolveSdk();
        }

        if (dependencyElement instanceof JpsLibraryDependency) {
            JpsLibraryDependency libraryDependency = (JpsLibraryDependency) dependencyElement;
            return libraryDependency.getLibrary();
        }

        return null;
    }

    private KotlinBuilderModuleScriptGenerator() {}
}
