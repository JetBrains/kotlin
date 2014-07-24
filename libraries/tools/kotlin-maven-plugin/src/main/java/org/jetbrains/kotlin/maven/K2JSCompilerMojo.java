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

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.k2js.config.MetaInfServices;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Converts Kotlin to JavaScript code
 *
 * @goal js
 * @phase compile
 * @noinspection UnusedDeclaration
 */
public class K2JSCompilerMojo extends KotlinCompileMojo {
    public static final String KOTLIN_JS_MAPS = "kotlin-maps.js";
    public static final String KOTLIN_JS_LIB = "kotlin-lib.js";
    public static final String KOTLIN_JS_LIB_ECMA3 = "kotlin-lib-ecma3.js";
    public static final String KOTLIN_JS_LIB_ECMA5 = "kotlin-lib-ecma5.js";

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
     * @parameter default-value="${project.build.directory}/js"
     * @parameter expression="${outputKotlinJSFile}"
     */
    private File outputKotlinJSDir;

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        if (appendLibraryJS != null && appendLibraryJS.booleanValue()) {
            try {
                Charset charset = Charset.defaultCharset();
                File file = new File(outputFile);
                String text = Files.toString(file, charset);
                StringBuilder builder = new StringBuilder();
                appendFile(KOTLIN_JS_LIB_ECMA3, builder);
                appendFile(KOTLIN_JS_LIB, builder);
                appendFile(KOTLIN_JS_MAPS, builder);
                builder.append("\n");
                builder.append(text);
                Files.write(builder.toString(), file, charset);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        if (copyLibraryJS != null && copyLibraryJS.booleanValue()) {
            getLog().info("Copying kotlin JS library to " + outputKotlinJSDir);

            copyJsLibraryFile(KOTLIN_JS_MAPS);
            copyJsLibraryFile(KOTLIN_JS_LIB);
            copyJsLibraryFile(KOTLIN_JS_LIB_ECMA3);
            copyJsLibraryFile(KOTLIN_JS_LIB_ECMA5);
        }
    }

    protected void appendFile(String jsLib, StringBuilder builder) throws MojoExecutionException {
        // lets copy the kotlin library into the output directory
        try {
            final InputStream inputStream = MetaInfServices.loadClasspathResource(jsLib);
            if (inputStream == null) {
                System.out.println("WARNING: Could not find " + jsLib + " on the classpath!");
            } else {
                InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
                    @Override
                    public InputStream getInput() throws IOException {
                        return inputStream;
                    }
                };
                String text = "\n" + FileUtil.loadTextAndClose(inputStream);
                builder.append(text);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected void copyJsLibraryFile(String jsLib) throws MojoExecutionException {
        // lets copy the kotlin library into the output directory
        try {
            outputKotlinJSDir.mkdirs();
            final InputStream inputStream = MetaInfServices.loadClasspathResource(jsLib);
            if (inputStream == null) {
                System.out.println("WARNING: Could not find " + jsLib + " on the classpath!");
            } else {
                InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
                    @Override
                    public InputStream getInput() throws IOException {
                        return inputStream;
                    }
                };
                Files.copy(inputSupplier, new File(outputKotlinJSDir, jsLib));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    protected void configureCompilerArguments(CommonCompilerArguments arguments) throws MojoExecutionException {
        super.configureCompilerArguments(arguments);

        if (arguments instanceof K2JSCompilerArguments) {
            K2JSCompilerArguments k2jsArgs = (K2JSCompilerArguments)arguments;
            k2jsArgs.outputFile = outputFile;
            if (getLog().isDebugEnabled()) {
                k2jsArgs.verbose = true;
            }
            List<String> sources = getSources();
            k2jsArgs.freeArgs.addAll(sources);
            getLog().info("Compiling Kotlin src from " + sources + " to JavaScript at: " + outputFile);
        }
    }

    @Override
    protected CommonCompilerArguments createCompilerArguments() {
        return new K2JSCompilerArguments();
    }

    @Override
    protected CLICompiler createCompiler() {
        return new K2JSCompiler();
    }
}
