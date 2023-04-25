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
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.maven.K2JVMCompileMojo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.maven.Util.joinArrays;
import static org.jetbrains.kotlin.maven.kapt.AnnotationProcessingManager.*;

/**
 * @noinspection UnusedDeclaration
 */
@Mojo(name = "kapt", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class KaptJVMCompilerMojo extends K2JVMCompileMojo {
    @Parameter
    private String[] annotationProcessors;

    @Parameter
    private List<DependencyCoordinate> annotationProcessorPaths;

    @Parameter
    private String aptMode = "stubsAndApt";

    @Parameter
    private boolean useLightAnalysis = true;

    @Parameter
    private boolean correctErrorTypes = false;

    @Parameter
    private boolean mapDiagnosticLocations = false;

    @Parameter
    private List<String> annotationProcessorArgs;

    @Parameter
    private List<String> javacOptions;

    // Components for AnnotationProcessingManager

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

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

        options.add(new KaptOption("aptMode", aptMode));
        options.add(new KaptOption("useLightAnalysis", useLightAnalysis));
        options.add(new KaptOption("correctErrorTypes", correctErrorTypes));
        options.add(new KaptOption("mapDiagnosticLocations", mapDiagnosticLocations));
        options.add(new KaptOption("processors", annotationProcessors));

        if (arguments.getVerbose()) {
            options.add(new KaptOption("verbose", true));
        }

        for (String entry : resolvedArtifacts.annotationProcessingClasspath) {
            options.add(new KaptOption("apclasspath", entry));
        }

        String sourceSetName = getSourceSetName();
        File sourcesDirectory = getGeneratedSourcesDirectory(project, sourceSetName);
        File kotlinSourcesDirectory = getGeneratedKotlinSourcesDirectory(project, sourceSetName);
        File classesDirectory = getGeneratedClassesDirectory(project, sourceSetName);
        File stubsDirectory = getStubsDirectory(project, sourceSetName);

        addKaptSourcesDirectory(sourcesDirectory.getPath());
        addKaptSourcesDirectory(kotlinSourcesDirectory.getPath());

        mkdirsSafe(classesDirectory);
        mkdirsSafe(stubsDirectory);
        mkdirsSafe(kotlinSourcesDirectory);

        options.add(new KaptOption("sources", sourcesDirectory.getAbsolutePath()));
        options.add(new KaptOption("classes", classesDirectory.getAbsolutePath()));
        options.add(new KaptOption("stubs", stubsDirectory.getAbsolutePath()));

        options.add(new KaptOption("javacArguments", encodeOptionList(parseOptionList(javacOptions))));

        Map<String, String> allApOptions = parseOptionList(annotationProcessorArgs);
        allApOptions.put("kapt.kotlin.generated", kotlinSourcesDirectory.getAbsolutePath());
        options.add(new KaptOption("apoptions", encodeOptionList(allApOptions)));

        return options;
    }

    @NotNull
    @Override
    protected ExitCode execCompiler(
            CLICompiler<K2JVMCompilerArguments> compiler,
            MessageCollector messageCollector,
            K2JVMCompilerArguments arguments,
            List<File> sourceRoots
    ) throws MojoExecutionException {
        // Annotation processing can't run incrementally so we need to clear the directory for our stubs and generated sources
        // TODO separate directories for the generated class files, and recreate the generated classfile dir also
        String sourceSetName = getSourceSetName();
        recreateDirectorySafe(getGeneratedSourcesDirectory(project, sourceSetName));
        recreateDirectorySafe(getStubsDirectory(project, sourceSetName));
        recreateDirectorySafe(getGeneratedKotlinSourcesDirectory(project, sourceSetName));

        return super.execCompiler(compiler, messageCollector, arguments, sourceRoots);
    }

    @Override
    protected List<String> getSourceFilePaths() {
        File generatedSourcesDirectory = getGeneratedSourcesDirectory(project, getSourceSetName());
        File generatedKotlinSourcesDirectory = getGeneratedKotlinSourcesDirectory(project, getSourceSetName());

        return super.getSourceFilePaths()
                .stream()
                .filter(path -> {
                    File pathFile = new File(path);
                    return !pathFile.equals(generatedSourcesDirectory)
                            && !pathFile.equals(generatedKotlinSourcesDirectory);
                })
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getClasspath() {
        File compileTargetDirectory = new File(this.output);

        // TODO it seems for me that the target directory should not be in the compile classpath
        // We filter out it here, but it's definitely a work-around.
        return super.getClasspath()
                .stream()
                .filter(path -> !new File(path).equals(compileTargetDirectory))
                .collect(Collectors.toList());
    }

    protected void addKaptSourcesDirectory(@NotNull String path) {
        project.addCompileSourceRoot(path);
    }

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JVMCompilerArguments arguments, @NotNull List<File> sourceRoots) throws MojoExecutionException {
        super.configureSpecificCompilerArguments(arguments, sourceRoots);

        AnnotationProcessingManager.ResolvedArtifacts resolvedArtifacts;

        try {
            resolvedArtifacts = getAnnotationProcessingManager().resolveAnnotationProcessors(annotationProcessorPaths);
        } catch (Exception e) {
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

        String jdkStringVersion = System.getProperty("java.specification.version");
        if (jdkStringVersion == null) return null;
        int jdkVersion;
        try {
            jdkVersion = Integer.parseInt(jdkStringVersion);
        } catch (NumberFormatException e) {
            // we got 1.8 or 1.6
            jdkVersion = 0;
        }
        if (jdkVersion >= 9) return null;

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

    @Override
    protected boolean isIncremental() {
        return false;
    }

    private void mkdirsSafe(@NotNull File directory) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            getLog().warn("Unable to create directory " + directory);
        }
    }

    private void deleteRecursivelySafe(@NotNull File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteRecursivelySafe(child);
                }
            }
        }

        if (!file.delete()) {
            getLog().warn("Unable to delete file " + file);
        }
    }

    private void recreateDirectorySafe(@NotNull File file) {
        if (file.exists()) {
            deleteRecursivelySafe(file);
        }
        mkdirsSafe(file);
    }

    private static Map<String, String> parseOptionList(@Nullable List<String> rawOptions) {
        Map<String, String> map = new LinkedHashMap<>();

        if (rawOptions == null) {
            return map;
        }

        for (String option : rawOptions) {
            if (option.isEmpty()) {
                continue;
            }

            int equalsIndex = option.indexOf("=");
            if (equalsIndex < 0) {
                map.put(option, "");
            } else {
                map.put(option.substring(0, equalsIndex).trim(), option.substring(equalsIndex + 1).trim());
            }
        }

        return map;
    }

    private static String encodeOptionList(Map<String, String> options) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
            ObjectOutputStream oos = new ObjectOutputStream(os);

            oos.writeInt(options.size());
            for (Map.Entry<String, String> entry : options.entrySet()) {
                oos.writeUTF(entry.getKey());
                oos.writeUTF(entry.getValue());
            }

            oos.flush();
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException e) {
            // Should not occur
            throw new RuntimeException(e);
        }
    }
}