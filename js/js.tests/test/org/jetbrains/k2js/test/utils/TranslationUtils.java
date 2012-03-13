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

package org.jetbrains.k2js.test.utils;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.test.config.TestConfig;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.utils.JetFileUtils.createPsiFileList;

/**
 * @author Pavel Talanov
 */
public final class TranslationUtils {

    private TranslationUtils() {
    }

    @Nullable
    private static /*var*/ K2JSTranslator translator = null;

    public static void translateFile(@NotNull Project project, @NotNull String inputFile,
                                     @NotNull String outputFile) throws Exception {
        translateFiles(project, Collections.singletonList(inputFile), outputFile);
    }

    public static void translateFiles(@NotNull Project project, @NotNull List<String> inputFiles,
                                      @NotNull String outputFile) throws Exception {
        List<JetFile> psiFiles = createPsiFileList(inputFiles, project);
        JsProgram program = getTranslator(project).generateProgram(psiFiles);
        FileWriter writer = new FileWriter(new File(outputFile));
        try {
            writer.write("\"use strict\";\n");
            writer.write(CodeGenerator.toString(program));
        } finally {
            writer.close();
        }
    }

    @NotNull
    private static K2JSTranslator getTranslator(@NotNull Project project) {
        if (translator == null) {
            translator = new K2JSTranslator(new TestConfig(project));
        }
        return translator;
    }
}
