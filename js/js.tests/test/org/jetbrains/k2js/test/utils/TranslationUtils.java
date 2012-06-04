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

import closurecompiler.internal.com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.test.config.TestConfig;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.utils.JetFileUtils.createPsiFileList;

/**
 * @author Pavel Talanov
 */
public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    private static final Map<EcmaVersion, K2JSTranslator> translators = Maps.newHashMap();

    public static void translateFile(@NotNull Project project, @NotNull String inputFile,
            @NotNull String outputFile, @NotNull MainCallParameters mainCallParameters, @NotNull EcmaVersion version) throws Exception {
        translateFiles(project, Collections.singletonList(inputFile), outputFile, mainCallParameters, version);
    }

    public static void translateFiles(@NotNull Project project, @NotNull List<String> inputFiles,
            @NotNull String outputFile, @NotNull MainCallParameters mainCallParameters, @NotNull EcmaVersion version) throws Exception {
        List<JetFile> psiFiles = createPsiFileList(inputFiles, project);
        JsProgram program = getTranslator(project, version).generateProgram(psiFiles, mainCallParameters);
        FileWriter writer = new FileWriter(new File(outputFile));
        try {
            writer.write(CodeGenerator.toString(program));
        }
        finally {
            writer.close();
        }
    }

    @NotNull
    private static K2JSTranslator getTranslator(@NotNull Project project, @NotNull EcmaVersion version) {
        K2JSTranslator translator = translators.get(version);
        if (translator == null) {
            translators.put(version, translator);
            translator = new K2JSTranslator(new TestConfig(project, version));
        }
        return translator;
    }
}
