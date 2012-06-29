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

package org.jetbrains.k2js.generate;

import com.google.dart.compiler.backend.js.JsSourceGenerationVisitor;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.util.DefaultTextOutput;
import com.google.dart.compiler.util.TextOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Pavel.Talanov
 */
public final class CodeGenerator {

    @NotNull
    private final TextOutput output = new DefaultTextOutput(false);

    private CodeGenerator() {
    }

    @NotNull
    public static String generateProgramToString(@NotNull JsProgram program, @Nullable List<String> rawStatements) {
        return (new CodeGenerator()).generateToString(program, rawStatements);
    }

    @NotNull
    private String generateToString(@NotNull JsProgram program, List<String> rawStatements) {
        generateCode(program);
        if (rawStatements != null) {
            for (String statement : rawStatements) {
                output.print(statement);
                output.newline();
            }
        }
        return output.toString();
    }

    private void generateCode(@NotNull JsProgram program) {
        JsSourceGenerationVisitor sourceGenerator =
                new JsSourceGenerationVisitor(output);
        program.traverse(sourceGenerator, null);
    }
}
