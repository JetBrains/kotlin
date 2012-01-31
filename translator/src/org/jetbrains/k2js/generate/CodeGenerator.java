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
