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
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.utils.LibraryFilePathsUtil;

import java.io.File;
import java.util.List;

abstract class StdLibTestBase extends SingleFileTranslationTest {

    protected StdLibTestBase() {
        super("stdlib/");
    }

    protected void performStdLibTest(@NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String sourceDir, @NotNull String... stdLibFiles) throws Exception {
        List<String> files = constructFilesToCompileList(sourceDir, stdLibFiles);
        compileFiles(ecmaVersions, files);
    }

    private void compileFiles(@NotNull Iterable<EcmaVersion> ecmaVersions, @NotNull List<String> files) throws Exception {
        List<String> libFiles = LibraryFilePathsUtil.getBasicLibraryFiles();
        for (EcmaVersion version : ecmaVersions) {
            String outputFilePath = getOutputFilePath(getTestName(false) + ".compiler.kt", version);
            invokeCompiler(files, libFiles, version, outputFilePath);
            performChecksOnGeneratedJavaScript(outputFilePath, version);
        }
    }

    /**
     * Strategy method allowing the generated JS file to be invoked
     */
    protected void performChecksOnGeneratedJavaScript(String path, EcmaVersion version) throws Exception {
    }

    protected String moduleIdFromOutputFile(String path) {
        String moduleId = new File(path).getName();
        if (moduleId.endsWith(".js")) {
            moduleId = moduleId.substring(0, moduleId.length() - 3);
        }
        return moduleId;
    }

    //TODO: reuse this in CompileMavenGeneratedJSLibrary
    private static void invokeCompiler(@NotNull List<String> files, @NotNull List<String> libFiles,
            @NotNull EcmaVersion version, @NotNull String outputFilePath) {
        K2JSCompiler compiler = new K2JSCompiler();
        K2JSCompilerArguments arguments = new K2JSCompilerArguments();
        arguments.outputFile = outputFilePath;
        arguments.freeArgs = files;
        arguments.verbose = true;
        arguments.libraryFiles = ArrayUtil.toStringArray(libFiles);
        System.out.println("Compiling with version: " + version + " to: " + arguments.outputFile);
        ExitCode answer = compiler.exec(System.out, arguments);
        assertEquals("Compile failed", ExitCode.OK, answer);
    }

    @NotNull
    private static List<String> constructFilesToCompileList(@NotNull String sourceDir, @NotNull String[] stdLibFiles) {
        List<String> files = filesFromDir(sourceDir, stdLibFiles);
        files.addAll(LibraryFilePathsUtil.getAdditionalLibraryFiles());
        return files;
    }

    @NotNull
    private static List<String> filesFromDir(@NotNull String sourceDir, @NotNull String[] stdLibFiles) {
        List<String> files = Lists.newArrayList();
        File stdlibDir = new File(sourceDir);
        assertTrue("Cannot find stdlib source: " + stdlibDir, stdlibDir.exists());
        for (String file : stdLibFiles) {
            files.add(new File(stdlibDir, file).getPath());
        }
        return files;
    }
}
