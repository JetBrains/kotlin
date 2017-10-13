package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKJavaVisitorVoid : JKJavaVisitor<Unit, Nothing?>, JKVisitorVoid {
    fun visitJavaField(javaField: JKJavaField) = visitDeclaration(javaField, null)
    override fun visitJavaField(javaField: JKJavaField, data: Nothing?) = visitJavaField(javaField)
    fun visitJavaMethod(javaMethod: JKJavaMethod) = visitDeclaration(javaMethod, null)
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: Nothing?) = visitJavaMethod(javaMethod)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop) = visitLoop(javaForLoop, null)
    override fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: Nothing?) = visitJavaForLoop(javaForLoop)
    fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression) = visitExpression(javaAssignmentExpression, null)
    override fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: Nothing?) = visitJavaAssignmentExpression(javaAssignmentExpression)
    fun visitJavaCall(javaCall: JKJavaCall) = visitCall(javaCall, null)
    override fun visitJavaCall(javaCall: JKJavaCall, data: Nothing?) = visitJavaCall(javaCall)
    fun visitJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier) = visitTypeIdentifier(javaTypeIdentifier, null)
    override fun visitJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier, data: Nothing?) = visitJavaTypeIdentifier(javaTypeIdentifier)
    fun visitJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression) = visitLiteralExpression(javaStringLiteralExpression, null)
    override fun visitJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression, data: Nothing?) = visitJavaStringLiteralExpression(javaStringLiteralExpression)
    fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier) = visitAccessModifier(javaAccessModifier, null)
    override fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: Nothing?) = visitJavaAccessModifier(javaAccessModifier)
}
