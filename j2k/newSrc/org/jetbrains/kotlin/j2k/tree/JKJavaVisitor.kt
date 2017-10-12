package org.jetbrains.kotlin.j2k.tree

interface JKJavaVisitor<R, D> : JKVisitor<R, D> {
    fun visitJavaField(javaField: JKJavaField, data: D): R = visitDeclaration(javaField, data)
    fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): R = visitDeclaration(javaMethod, data)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: D): R = visitLoop(javaForLoop, data)
    fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: D): R = visitExpression(javaAssignmentExpression, data)
    fun visitJavaCall(javaCall: JKJavaCall, data: D): R = visitCall(javaCall, data)
    fun visitJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier, data: D): R = visitTypeIdentifier(javaTypeIdentifier, data)
    fun visitJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression, data: D): R = visitLiteralExpression(javaStringLiteralExpression, data)
    fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: D): R = visitAccessModifier(javaAccessModifier, data)
}
