/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Pavel.Talanov
 */
public final class CodeGenerator {

    @NotNull
    private final TextOutput output = new DefaultTextOutput(false);

    public CodeGenerator() {
    }

    public void generateToFile(@NotNull JsProgram program, @NotNull File file) throws IOException {
        generateCode(program);
        FileWriter writer = new FileWriter(file);
        writer.write(output.toString());
        writer.close();
    }

    @NotNull
    public String generateToString(@NotNull JsProgram program) {
        generateCode(program);
        return output.toString();
    }

    private void generateCode(@NotNull JsProgram program) {
        JsSourceGenerationVisitor sourceGenerator =
                new JsSourceGenerationVisitor(output);
        program.traverse(sourceGenerator, null);
    }
}
