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

package org.jetbrains.kotlin.maven;

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.CompilerArguments;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import org.jetbrains.jet.cli.js.K2JSCompilerArguments;
import org.jetbrains.jet.internal.com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.k2js.config.MetaInfServices;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Converts Kotlin to JavaScript code
 *
 * @goal js
 * @phase compile
 * @noinspection UnusedDeclaration
 */
public class K2JSCompilerMojo extends KotlinCompileMojo {
    public static final String KOTLIN_JS_LIB = "kotlin-lib.js";

    /**
     * The output JS file name
     *
     * @required
     * @parameter default-value="${project.build.directory}/js/${project.artifactId}.js"
     */
    private String outputFile;

    /**
     * The output Kotlin JS file
     *
     * @required
     * @parameter default-value="${project.build.directory}/js/kotlin-lib.js"
     * @parameter expression="${outputKotlinJSFile}"
     */
    private File outputKotlinJSFile;

    /**
     * Whether to copy the kotlin-lib.js file to the output directory
     *
     * @parameter default-value="true"
     * @parameter expression="${copyLibraryJS}"
     */
    private Boolean copyLibraryJS;

    /**
     * Whether to copy the kotlin-lib.js file to the output directory
     *
     * @parameter default-value="false"
     * @parameter expression="${appendLibraryJS}"
     */
    private Boolean appendLibraryJS;

    /**
     * Whether verbose logging is enabled or not.
     *
     * @parameter default-value="false"
     * @parameter expression="${verbose}"
     */
    private Boolean verbose;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        if (copyLibraryJS) {
            getLog().info("Copying kotlin JS library to " + outputKotlinJSFile);

            // lets copy the kotlin library into the output directory
            try {
                File parentFile = outputKotlinJSFile.getParentFile();
                parentFile.mkdirs();
                final InputStream inputStream = MetaInfServices.loadClasspathResource(KOTLIN_JS_LIB);
                if (inputStream == null) {
                    System.out.println("WARNING: Could not find " + KOTLIN_JS_LIB + " on the classpath!");
                } else {
                    InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
                        @Override
                        public InputStream getInput() throws IOException {
                            return inputStream;
                        }
                    };
                    Files.copy(inputSupplier, outputKotlinJSFile);
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        if (appendLibraryJS) {
            getLog().info("Appending Kotlin Library JS to the generated file " + outputFile);

            // lets copy the kotlin library into the output directory
            try {
                final InputStream inputStream = MetaInfServices.loadClasspathResource(KOTLIN_JS_LIB);
                if (inputStream == null) {
                    System.out.println("WARNING: Could not find " + KOTLIN_JS_LIB + " on the classpath!");
                } else {
                    InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
                        @Override
                        public InputStream getInput() throws IOException {
                            return inputStream;
                        }
                    };
                    String text = "\n" + FileUtil.loadTextAndClose(inputStream);
                    Charset charset = Charset.defaultCharset();
                    Files.append(text, new File(outputFile), charset);
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

    @Override
    protected void configureCompilerArguments(CompilerArguments arguments) throws MojoExecutionException {
        super.configureCompilerArguments(arguments);

        if (arguments instanceof K2JSCompilerArguments) {
            K2JSCompilerArguments k2jsArgs = (K2JSCompilerArguments)arguments;
            k2jsArgs.outputFile = outputFile;
            if (verbose != null) {
                k2jsArgs.verbose = verbose;
            }
            if (sources.size() > 0) {
                k2jsArgs.sourceFiles = sources;
            }
        }
        getLog().info("Compiling Kotlin src from " + arguments.getSrc() + " to JavaScript at: " + outputFile);
    }

    @Override
    protected CompilerArguments createCompilerArguments() {
        return new K2JSCompilerArguments();
    }

    @Override
    protected CLICompiler createCompiler() {
        return new K2JSCompiler();
    }
}
