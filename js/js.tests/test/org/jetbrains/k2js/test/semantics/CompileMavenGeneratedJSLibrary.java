/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.semantics;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.SingleFileTranslationTest;

import java.io.File;
import java.util.List;

/**
 * Lets test compiling all the JS code thats used by the library-js-library from the maven build
 *
 * This test case isn't run by default but can only be ran after the maven build
 * has completed so that there are various kotlin files to be compiled
 */
public class CompileMavenGeneratedJSLibrary extends SingleFileTranslationTest {

    protected final String generatedJsDir = "libraries/tools/kotlin-js-library/target/";
    protected String generatedJsDefinitionsDir = generatedJsDir + "generated-js-definitions";
    protected File generatedJsLibraryDir = new File( generatedJsDir + "generated-js-library");

    public CompileMavenGeneratedJSLibrary() {
        super("kotlin-js-library/");
    }

    public static void main(String[] args) throws Exception {
        CompileMavenGeneratedJSLibrary test = new CompileMavenGeneratedJSLibrary();
        test.DISABLED_testGenerateTestCase();
    }

    public void testDummy() {
    }

    public void DISABLED_testGenerateTestCase() throws Exception {
        if (generatedJsLibraryDir.exists() && generatedJsLibraryDir.isDirectory()) {
            generateJavaScriptFiles(DEFAULT_ECMA_VERSIONS,
                                    "libraries/stdlib/test",
                                    "collections/ArraysTest.kt",
                                    "dom/DomTest.kt",
                                    "js/MapTest.kt",
                                    "js/JsDomTest.kt",
                                    "collections/FunctionIteratorTest.kt",
                                    "collections/IteratorsTest.kt",
                                    "GetOrElseTest.kt",
                                    "collections/ListTest.kt",
                                    "collections/SetTest.kt",
                                    "text/StringTest.kt");

        } else {
            System.out.println("Warning " + generatedJsLibraryDir + " does not exist - I guess you've not run the maven build in library/ yet?");
        }
    }

    protected void generateJavaScriptFiles(@NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String sourceDir, @NotNull String... stdLibFiles) throws Exception {
        List<String> files = Lists.newArrayList();


        // now lets add all the files from the definitions and library
        //addAllSourceFiles(files, generatedJsDefinitionsDir);
        addAllSourceFiles(files, generatedJsLibraryDir);


        File stdlibDir = new File(sourceDir);
        assertTrue("Cannot find stdlib test source: " + stdlibDir, stdlibDir.exists());
        for (String file : stdLibFiles) {
            files.add(new File(stdlibDir, file).getPath());
        }

        // now lets try invoke the compiler
        for (EcmaVersion version : ecmaVersions) {
            K2JSCompiler compiler = new K2JSCompiler();
            K2JSCompilerArguments arguments = new K2JSCompilerArguments();
            arguments.outputFile = getOutputFilePath(getTestName(false) + ".compiler.kt", version);
            arguments.freeArgs = files;
            arguments.verbose = true;
            arguments.libraryFiles = new String[] {generatedJsDefinitionsDir};
            System.out.println("Compiling with version: " + version + " to: " + arguments.outputFile);
            ExitCode answer = compiler.exec(System.out, arguments);
            assertEquals("Compile failed", ExitCode.OK, answer);
        }
    }

    private static void addAllSourceFiles(List<String> files, File dir) {
        File[] children = dir.listFiles();
        if (children != null && children.length > 0) {
            for (File child : children) {
                if (child.isDirectory()) {
                    addAllSourceFiles(files, child);
                } else {
                    String name = child.getName();
                    if (name.toLowerCase().endsWith(".kt")) {
                        files.add(child.getPath());
                    }
                }
            }
        }
    }
}
