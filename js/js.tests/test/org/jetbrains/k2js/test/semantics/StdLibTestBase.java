/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.utils.TranslationUtils;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

abstract class StdLibTestBase extends SingleFileTranslationTest {

    protected StdLibTestBase() {
        super("stdlib/");
    }

    protected void performStdLibTest(@NotNull EnumSet<EcmaVersion> ecmaVersions,
            @NotNull String sourceDir, @NotNull String... stdLibFiles) throws Exception {
        List<String> files = Lists.newArrayList();

        File stdlibDir = new File(sourceDir);
        assertTrue("Cannot find stdlib source: " + stdlibDir, stdlibDir.exists());
        for (String file : stdLibFiles) {
            files.add(new File(stdlibDir, file).getPath());
        }


        // lets add the standard JS library files
        Iterable<String> names = Config.LIB_FILE_NAMES_DEPENDENT_ON_STDLIB;
        for (String libFileName : names) {
            System.out.println("Compiling " + libFileName);
            files.add(Config.LIBRARIES_LOCATION + libFileName);
        }

        // lets add the standard Kotlin library files
        for (String libFileName : Config.STDLIB_FILE_NAMES) {
            System.out.println("Compiling " + libFileName);
            files.add(Config.STDLIB_LOCATION + libFileName);
        }
        for (EcmaVersion version : ecmaVersions) {
            String outputFilePath = getOutputFilePath(getTestName(false) + ".compiler.kt", version);
            TranslationUtils.translateFiles(getProject(), files, outputFilePath, MainCallParameters.noCall(), version);
        }
    }
}
