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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.CompilerPlugin;
import org.jetbrains.jet.compiler.CompilerPluginContext;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.facade.K2JSTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiles Kotlin code to JavaScript
 */
public class K2JSCompilerPlugin implements CompilerPlugin {
    private String outFile = "target/js/program.js";

    public void processFiles(CompilerPluginContext context) {
        if (context != null) {
            Project project = context.getProject();
            BindingContext bindingContext = context.getContext();
            List<JetFile> sources = context.getFiles();

            if (bindingContext != null && sources != null && project != null) {
                Config config = new Config(project) {
                    @NotNull
                    @Override
                    public List<JetFile> getLibFiles() {
                        return new ArrayList<JetFile>();
                    }
                };

                K2JSTranslator translator = new K2JSTranslator(config);
                final String code = translator.generateProgramCode(sources);

                File file = new File(outFile);

                try {
                    Files.createParentDirs(file);
                    Files.write(code, file, Charset.forName("UTF-8"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }
}
