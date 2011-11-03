package org.jetbrains.k2js.generate;

import com.google.dart.compiler.backend.js.JsSourceGenerationVisitor;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.util.DefaultTextOutput;
import com.google.dart.compiler.util.TextOutput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Pavel.Talanov
 */
public final class CodeGenerator {

    private final TextOutput output = new DefaultTextOutput(false);

    public CodeGenerator() {

    }

    public void generateToConsole(JsProgram program) {
        generateCode(program);
        System.out.println(output.toString());
    }

    private void generateCode(JsProgram program) {
        JsSourceGenerationVisitor sourceGenerator =
                        new JsSourceGenerationVisitor(output);
        program.traverse(sourceGenerator, null);
    }

    public void generateToFile(JsProgram program, File file) throws IOException {
        generateCode(program);
        FileWriter writer = new FileWriter(file);
        writer.write(output.toString());
        writer.close();
    }
}
