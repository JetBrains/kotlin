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
import com.intellij.openapi.util.io.FileUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import org.jetbrains.k2js.config.MetaInfServices;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Converts Kotlin to JavaScript code
 *
 * @goal js
 * @phase compile
 * @noinspection UnusedDeclaration
 */
public class K2JSCompilerMojo extends KotlinCompileMojoBase<K2JSCompilerArguments> {
    public static final String KOTLIN_JS_MAPS = "kotlin-maps.js";
    public static final String KOTLIN_JS_LONG = "kotlin-long.js";
    public static final String KOTLIN_JS_LIB = "kotlin-lib.js";
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

        if (appendLibraryJS != null && appendLibraryJS) {
            try {
                Charset charset = Charset.defaultCharset();
                File file = new File(outputFile);
                String text = Files.toString(file, charset);
                StringBuilder builder = new StringBuilder();
                appendFile(KOTLIN_JS_LIB_ECMA5, builder);
                appendFile(KOTLIN_JS_LIB, builder);
                appendFile(KOTLIN_JS_MAPS, builder);
                appendFile(KOTLIN_JS_LONG, builder);
                builder.append("\n");
                builder.append(text);
                Files.write(builder.toString(), file, charset);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        if (copyLibraryJS != null && copyLibraryJS) {
            LOG.info("Copying kotlin JS library to " + outputKotlinJSDir);

            copyJsLibraryFile(KOTLIN_JS_MAPS);
            copyJsLibraryFile(KOTLIN_JS_LONG);
            copyJsLibraryFile(KOTLIN_JS_LIB);
            copyJsLibraryFile(KOTLIN_JS_LIB_ECMA5);
        }
    }

    private void appendFile(String jsLib, StringBuilder builder) throws MojoExecutionException {
        // lets copy the kotlin library into the output directory
        try {
            final InputStream inputStream = MetaInfServices.loadClasspathResource(jsLib);
            if (inputStream == null) {
                LOG.warn("Could not find " + jsLib + " on the classpath!");
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

    private void copyJsLibraryFile(String jsLib) throws MojoExecutionException {
        try {
            final InputStream inputStream = MetaInfServices.loadClasspathResource(jsLib);
            if (inputStream == null) {
                LOG.warn("Could not find " + jsLib + " on the classpath!");
            } else {
                if (!outputKotlinJSDir.exists() && !outputKotlinJSDir.mkdirs()) {
                    throw new MojoExecutionException("Could not create output directory '" + outputKotlinJSDir + "'.");
                }

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
    protected void configureSpecificCompilerArguments(@NotNull K2JSCompilerArguments arguments) throws MojoExecutionException {
        arguments.outputFile = outputFile;
    }

    @NotNull
    @Override
    protected K2JSCompilerArguments createCompilerArguments() {
        return new K2JSCompilerArguments();
    }

    @NotNull
    @Override
    protected K2JSCompiler createCompiler() {
        return new K2JSCompiler();
    }
}
