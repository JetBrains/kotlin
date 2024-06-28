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

package org.jetbrains.kotlin.maven;

import com.intellij.openapi.util.text.StringUtil;
import kotlin.collections.CollectionsKt;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.utils.JsLibraryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Converts Kotlin to JavaScript code
 *
 * @noinspection UnusedDeclaration
 */
@Mojo(name = "js", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class K2JSCompilerMojo extends KotlinCompileMojoBase<K2JSCompilerArguments> {

    private static final String OUTPUT_DIRECTORIES_COLLECTOR_PROPERTY_NAME = "outputDirectoriesCollector";
    private static final Lock lock = new ReentrantLock();

    /**
     * The output JS file name
     */
    @Parameter(defaultValue = "${project.build.directory}/js/${project.artifactId}.js", required = true)
    private String outputFile;

    /**
     * Flag enables or disables .meta.js and .kjsm files generation, used to create libraries
     */
    @Parameter(defaultValue = "true")
    private boolean metaInfo;

    /**
     * Flags enables or disable source map generation
     */
    @Parameter(defaultValue = "false")
    private boolean sourceMap;

    @Parameter
    private String sourceMapPrefix;

    @Parameter(defaultValue = "inlining")
    private String sourceMapEmbedSources;

    /**
     * Main invocation behaviour. Possible values are <b>call</b> and <b>noCall</b>.
     */
    @Parameter
    private String main;

    /**
     * <p>Specifies which JS module system to generate compatible sources for. Options are:</p>
     * <ul>
     *     <li><b>amd</b> &mdash;
     *       <a href="https://github.com/amdjs/amdjs-api/wiki/AMD"></a>Asynchronous Module System</a>;</li>
     *     <li><b>commonjs</b> &mdash; npm/CommonJS conventions based on synchronous <code>require</code>
     *       function;</li>
     *     <li><b>plain</b> (default) &mdash; no module system, keep all modules in global scope;</li>
     *     <li><b>umd</b> &mdash; Universal Module Definition, stub wrapper that detects current
     *       module system in runtime and behaves as <code>plain</code> if none detected.</li>
     * </ul>
     */
    @Parameter(defaultValue = "plain")
    private String moduleKind;

    @Parameter(defaultValue = "false")
    private boolean useIrBackend;

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JSCompilerArguments arguments, @NotNull List<File> sourceRoots) throws MojoExecutionException {
        arguments.setOutputDir(new File(outputFile).getParent());
        arguments.setModuleKind(moduleKind);
        arguments.setMain(main);
        arguments.setIrOnly(useIrBackend);
        arguments.setIrProduceJs(useIrBackend);
        arguments.setIrProduceKlibDir(useIrBackend);

        List<String> libraries;
        try {
            libraries = getKotlinJavascriptLibraryFiles();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Unresolved dependencies", e);
        }
        getLog().debug("libraries: " + libraries);
        arguments.setLibraries(StringUtil.join(libraries, File.pathSeparator));

        arguments.setSourceMap(sourceMap);
        arguments.setSourceMapPrefix(sourceMapPrefix);
        arguments.setSourceMapEmbedSources(sourceMapEmbedSources);

        if (outputFile != null) {
            ConcurrentMap<String, List<String>> collector = getOutputDirectoriesCollector();
            String key = project.getArtifactId();
            List<String> paths = collector.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
            paths.add(new File(outputFile).getParent());
        }

        StringBuilder sourceMapSourceRoots = new StringBuilder();
        if (!sourceRoots.isEmpty()) {
            sourceMapSourceRoots.append(sourceRoots.get(0).getAbsolutePath());
            for (int i = 1; i < sourceRoots.size(); ++i) {
                sourceMapSourceRoots.append(File.pathSeparator);
                sourceMapSourceRoots.append(sourceRoots.get(i).getAbsolutePath());
            }
        }

        arguments.setSourceMapBaseDirs(sourceMapSourceRoots.toString());
    }

    protected List<String> getClassPathElements() throws DependencyResolutionRequiredException {
        return project.getCompileClasspathElements();
    }

    private boolean checkIsKotlinJavascriptLibrary(File file) {
        return useIrBackend ? JsLibraryUtils.isKotlinJavascriptIrLibrary(file) : JsLibraryUtils.isKotlinJavascriptLibrary(file);
    }

    /**
     * Returns all Kotlin Javascript dependencies that this project has, including transitive ones.
     *
     * @return array of paths to kotlin javascript libraries
     */
    @NotNull
    private List<String> getKotlinJavascriptLibraryFiles() throws DependencyResolutionRequiredException {
        List<String> libraries = new ArrayList<>();

        for (String path : getClassPathElements()) {
            File file = new File(path);

            if (file.exists() && checkIsKotlinJavascriptLibrary(file)) {
                libraries.add(file.getAbsolutePath());
            }
            else {
                getLog().debug("artifact " + file.getAbsolutePath() + " is not a Kotlin Javascript Library");
            }
        }

        for (List<String> paths : getOutputDirectoriesCollector().values()) {
            for (String path : paths) {
                File file = new File(path);

                if (file.exists() && checkIsKotlinJavascriptLibrary(file)) {
                    libraries.add(file.getAbsolutePath());
                }
                else {
                    getLog().debug("JS output directory missing: " + file);
                }
            }
        }

        return libraries;
    }

    @NotNull
    @Override
    protected K2JSCompilerArguments createCompilerArguments() {
        return new K2JSCompilerArguments();
    }

    @Override
    protected List<String> getRelatedSourceRoots(MavenProject project) {
        return project.getCompileSourceRoots();
    }

    @NotNull
    @Override
    protected K2JSCompiler createCompiler() {
        return new K2JSCompiler();
    }

    @SuppressWarnings("unchecked")
    protected ConcurrentMap<String, List<String>> getOutputDirectoriesCollector() {
        lock.lock();
        try {
            ConcurrentMap<String, List<String>> collector = (ConcurrentMap<String, List<String>>) getPluginContext().get(OUTPUT_DIRECTORIES_COLLECTOR_PROPERTY_NAME);
            if (collector == null) {
                collector = new ConcurrentSkipListMap<>();
                getPluginContext().put(OUTPUT_DIRECTORIES_COLLECTOR_PROPERTY_NAME, collector);
            }

            return collector;
        } finally {
            lock.unlock();
        }
    }
}
