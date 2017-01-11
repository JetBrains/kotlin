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

package org.jetbrains.kotlin.maven;

import com.intellij.util.ArrayUtil;
import kotlin.text.StringsKt;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.js.K2JSCompiler;
import org.jetbrains.kotlin.utils.LibraryUtils;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils;
import org.jetbrains.kotlin.js.JavaScript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
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
     * Flag enables or disables metafile generation
     */
    @Parameter(defaultValue = "true")
    private boolean metaInfo;

    /**
     * Flag enables or disables kjsm generation
     */
    @Parameter(defaultValue = "true")
    private boolean kjsm;

    /**
     * Flags enables or disable source map generation
     */
    @Parameter(defaultValue = "false")
    private boolean sourceMap;

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

    @Override
    protected void configureSpecificCompilerArguments(@NotNull K2JSCompilerArguments arguments) throws MojoExecutionException {
        arguments.outputFile = outputFile;
        arguments.noStdlib = true;
        arguments.metaInfo = metaInfo;
        arguments.kjsm = kjsm;
        arguments.moduleKind = moduleKind;

        List<String> libraries = getKotlinJavascriptLibraryFiles();
        getLog().debug("libraryFiles: " + libraries);
        arguments.libraryFiles = ArrayUtil.toStringArray(libraries);

        arguments.sourceMap = sourceMap;

        Set<String> collector = getOutputDirectoriesCollector();

        if (outputFile != null) {
            collector.add(new File(outputFile).getParent());
        }
        if (metaInfo) {
            String output = com.google.common.base.Objects.firstNonNull(outputFile, ""); // fqname here because of J8 compatibility issues
            String metaFile = StringsKt.substringBeforeLast(output, JavaScript.DOT_EXTENSION, output) + KotlinJavascriptMetadataUtils.META_JS_SUFFIX;
            collector.add(new File(metaFile).getParent());
        }
    }

    /**
     * Returns all Kotlin Javascript dependencies that this project has, including transitive ones.
     *
     * @return array of paths to kotlin javascript libraries
     */
    @NotNull
    private List<String> getKotlinJavascriptLibraryFiles() {
        List<String> libraries = new ArrayList<String>();

        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getScope().equals(Artifact.SCOPE_COMPILE)) {
                File file = artifact.getFile();
                if (LibraryUtils.isKotlinJavascriptLibrary(file)) {
                    libraries.add(file.getAbsolutePath());
                }
                else {
                    getLog().warn("artifact " + artifact + " is not a Kotlin Javascript Library");
                }
            }
        }

        for (String file : getOutputDirectoriesCollector()) {
            if (new File(file).exists()) {
                libraries.add(file);
            }
            else {
                getLog().warn("JS output directory missing: " + file);
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
    private Set<String> getOutputDirectoriesCollector() {
        lock.lock();
        try {
            Set<String> collector = (Set<String>) getPluginContext().get(OUTPUT_DIRECTORIES_COLLECTOR_PROPERTY_NAME);
            if (collector == null) {
                collector = new ConcurrentSkipListSet<String>();
                getPluginContext().put(OUTPUT_DIRECTORIES_COLLECTOR_PROPERTY_NAME, collector);
            }

            return collector;
        } finally {
            lock.unlock();
        }
    }
}
