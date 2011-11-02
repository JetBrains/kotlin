package org.jetbrains.k2js.generate;

import com.google.dart.compiler.backend.js.JsSourceGenerationVisitor;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.util.DefaultTextOutput;
import com.google.dart.compiler.util.TextOutput;

/**
 * @author Pavel.Talanov
 */
public final class CodeGenerator {

    private final TextOutput output = new DefaultTextOutput(false);

    public CodeGenerator() {

    }

    public void generate(JsProgram program) {
        JsSourceGenerationVisitor sourceGenerator =
                        new JsSourceGenerationVisitor(output);
        program.traverse(sourceGenerator, null);
        System.out.println(output.toString());
    }
}
