package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.DefaultTextOutput;
import com.google.dart.compiler.util.TextOutput;

public class Test {

    public static void main(String[] args) {
        JsProgram program = new JsProgram("test");
        JsNumberLiteral number = program.getNumberLiteral(2443.3);
        //JsGlobalBlock globalBlock = new JsGlobalBlock(number);
        TextOutput output = new DefaultTextOutput(false);
        JsNameRef varStatement = new JsNameRef("variable");
        JsBinaryOperation plus = new JsBinaryOperation(JsBinaryOperator.ADD, number, number);
        JsBinaryOperation assign = new JsBinaryOperation(JsBinaryOperator.ASG, varStatement, plus);

        JsSourceGenerationVisitor sourceGenerator =
                new JsSourceGenerationVisitor(output);
        sourceGenerator.visit(assign, null);
        System.out.println(output.toString());
    }
}
