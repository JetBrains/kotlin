/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.maven.kapt;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.compiler.DependencyCoordinate;
import org.apache.maven.plugins.annotations.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.maven.K2JVMCompileMojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.maven.kapt.AnnotationProcessingManager.*;

/** @noinspection UnusedDeclaration */
@Mojo(name = "kapt", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class KaptJVMCompilerMojo extends K2JVMCompileMojo {
    @Parameter
    private String[] annotationProcessors;

    @Parameter
    private List<DependencyCoordinate> annotationProcessorPaths;

    @Parameter
    private boolean useLightAnalysis = true;

    @Parameter
    private boolean correctErrorTypes = false;

    // Components for AnnotationProcessingManager

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Component
    private ResolutionErrorHandler resolutionErrorHandler;

    private AnnotationProcessingManager cachedAnnotationProcessingManager;

    private AnnotationProcessingManager getAnnotationProcessingManager() {
        if (cachedAnnotationProcessingManager != null) {
            return cachedAnnotationProcessingManager;
        }

        cachedAnnotationProcessingManager = new AnnotationProcessingManager(
                artifactHandlerManager, session, project, system, resolutionErrorHandler);
        return cachedAnnotationProcessingManager;
    }

    @NotNull
    private List<KaptOption> getKaptOptions(
            @NotNull K2JVMCompilerArguments arguments,
            @NotNull AnnotationProcessingManager.ResolvedArtifacts resolvedArtifacts
    ) {
        List<KaptOption> options = new ArrayList<>();

        options.add(new KaptOption("aptOnly", true));
        options.add(new KaptOption("useLightAnalysis", useLightAnalysis));
        options.add(new KaptOption("correctErrorTypes", correctErrorTypes));
        options.add(new KaptOption("processors", annotationProcessors));

        if (arguments.getVerbose()) {
            options.add(new KaptOption("verbose", true));
        }

        for (String entry : resolvedArtifacts.annotationProcessingClasspath) {
            options.add(new KaptOption("apclasspath", entry));
        }

        String sourceSetName = getSourceSetName();
        File sourcesDirectory = getGeneratedSourcesDirectory(project, sourceSetName);
        File classesDirectory = getGeneratedClassesDirectory(project, sourceSetName);
        File stubsDirectory = getStubsDirectory(project, sourceSetName);

        addKaptSourcesDirectory(sourcesDirectory.getPath());

        mkdirsSafe(sourcesDirectory);
        mkdirsSafe(classesDirectory);
        mkdirsSafe(stubsDirectory);

        options.add(new KaptOption("sources", sourcesDirectory.getAbsolutePath()));
        options.add(new KaptOption("classes", classesDirectory.getAbsolutePath()));
        options.add(new KaptOption("stubs", stubsDirectory.getAbsolutePath()));

        return options;
    }

    protected void addKaptSourcesDirectory(@NotNull String path) {
        project.addCompileSourceRoot(path);
    }

    private void mkdirsSafe(@NotNull File directory) {
        if (!directory.mkdirs()) {
            getLog().warn("Unable to create directory " + directory);
        }
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JVMCompilerArguments arguments, @NotNull List<File> sourceRoots) throws MojoExecutionException {
        super.configureSpecificCompilerArguments(arguments, sourceRoots);

        AnnotationProcessingManager.ResolvedArtifacts resolvedArtifacts;

        try {
            resolvedArtifacts = getAnnotationProcessingManager().resolveAnnotationProcessors(annotationProcessorPaths);
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error while processing kapt options", e);
        }

        String[] kaptOptions = renderKaptOptions(getKaptOptions(arguments, resolvedArtifacts));
        arguments.setPluginOptions(joinArrays(arguments.getPluginOptions(), kaptOptions));

        String jdkToolsJarPath = getJdkToolsJarPath();
        arguments.setPluginClasspaths(
                joinArrays(
                        arguments.getPluginClasspaths(),
                        (jdkToolsJarPath == null)
                                ? new String[]{resolvedArtifacts.kaptCompilerPluginArtifact}
                                : new String[]{jdkToolsJarPath, resolvedArtifacts.kaptCompilerPluginArtifact}
                )
        );
    }

    @Nullable
    private String getJdkToolsJarPath() {
        String javaHomePath = System.getProperty("java.home");
        if (javaHomePath == null || javaHomePath.isEmpty()) {
            getLog().warn("Can't determine Java home, 'java.home' property does not exist");
            return null;
        }
        File javaHome = new File(javaHomePath);
        File toolsJar = new File(javaHome, "lib/tools.jar");
        if (toolsJar.exists()) {
            return toolsJar.getAbsolutePath();
        }

        // We might be inside jre.
        if (javaHome.getName().equals("jre")) {
            toolsJar = new File(javaHome.getParent(), "lib/tools.jar");
            if (toolsJar.exists()) {
                return toolsJar.getAbsolutePath();
            }
        }

        getLog().debug(toolsJar.getAbsolutePath() + " does not exist");
        getLog().warn("'tools.jar' was not found, kapt may work unreliably");
        return null;
    }

    @NotNull
    private String[] renderKaptOptions(@NotNull List<KaptOption> options) {
        String[] result = new String[options.size()];
        int i = 0;
        for (KaptOption option : options) {
            result[i++] = option.toString();
        }
        return result;
    }

    @NotNull
    private String[] joinArrays(@Nullable String[] first, @Nullable String[] second) {
        if (first == null) {
            first = new String[0];
        }
        if (second == null) {
            second = new String[0];
        }

        String[] result = new String[first.length + second.length];

        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);

        return result;
    }

    @Override
    protected boolean isIncremental() {
        return false;
    }
}