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

package org.jetbrains.k2js.test.semantics;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.js.K2JSCompiler;
import org.jetbrains.jet.cli.js.K2JSCompilerArguments;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.rhino.RhinoFunctionNativeObjectResultChecker;
import org.jetbrains.k2js.utils.JetFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 */
public final class StdLibToJSTest extends SingleFileTranslationTest {

    public StdLibToJSTest() {
        super("stdlib/");
    }

    public void testCompileStandardLibraryFiles() throws Exception {

        generateJavaScriptFiles(MainCallParameters.noCall(), EcmaVersion.all(),
                                "kotlin/Preconditions.kt",
                                "kotlin/dom/Dom.kt",
                                "kotlin/support/AbstractIterator.kt"
        );
    }

    protected void generateJavaScriptFiles(@NotNull MainCallParameters mainCallParameters,
            @NotNull EnumSet<EcmaVersion> ecmaVersions,
            String... stdLibFiles) throws Exception {
        List<String> files = Lists.newArrayList();


        File stdlibDir = new File("libraries/stdlib/src");
        assertTrue("Cannot find stdlib source: " + stdlibDir, stdlibDir.exists());
        for (String file : stdLibFiles) {
            files.add(new File(stdlibDir, file).getPath());
        }
        generateJavaScriptFiles(files, getTestName(false) + ".kt", mainCallParameters, ecmaVersions);

        /*
        runRhinoTests(getOutputFilePaths(kotlinFilename, ecmaVersions),
                      new RhinoFunctionNativeObjectResultChecker("test.browser", "foo", "Some Dynamically Created Content!!!"));
        */

        // lets add the standard JS library files
        for (String libFileName : Config.LIB_FILE_NAMES) {
            files.add(Config.LIBRARIES_LOCATION + libFileName);
        }
        // now lets try invoke the compiler
        for (EcmaVersion version : ecmaVersions) {
            System.out.println("Compiling with version: " + version);
            K2JSCompiler compiler = new K2JSCompiler();
            K2JSCompilerArguments arguments = new K2JSCompilerArguments();
            arguments.outputFile = getOutputFilePath(getTestName(false) + ".compiler.kt", version);
            arguments.sourceFiles = files;
            arguments.verbose = true;
            compiler.exec(System.out, arguments);
        }
    }
}
