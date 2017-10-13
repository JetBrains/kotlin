package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKJavaTransformerVoid : JKJavaTransformer<Nothing?>, JKTransformerVoid {
    fun transformJavaField(javaField: JKJavaField) = transformDeclaration(javaField, null)
    override fun transformJavaField(javaField: JKJavaField, data: Nothing?) = transformJavaField(javaField)
    fun transformJavaMethod(javaMethod: JKJavaMethod) = transformDeclaration(javaMethod, null)
    override fun transformJavaMethod(javaMethod: JKJavaMethod, data: Nothing?) = transformJavaMethod(javaMethod)
    fun transformJavaForLoop(javaForLoop: JKJavaForLoop) = transformLoop(javaForLoop, null)
    override fun transformJavaForLoop(javaForLoop: JKJavaForLoop, data: Nothing?) = transformJavaForLoop(javaForLoop)
    fun transformJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression) = transformExpression(javaAssignmentExpression, null)
    override fun transformJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: Nothing?) = transformJavaAssignmentExpression(javaAssignmentExpression)
    fun transformJavaCall(javaCall: JKJavaCall) = transformCall(javaCall, null)
    override fun transformJavaCall(javaCall: JKJavaCall, data: Nothing?) = transformJavaCall(javaCall)
    fun transformJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier) = transformTypeIdentifier(javaTypeIdentifier, null)
    override fun transformJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier, data: Nothing?) = transformJavaTypeIdentifier(javaTypeIdentifier)
    fun transformJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression) = transformLiteralExpression(javaStringLiteralExpression, null)
    override fun transformJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression, data: Nothing?) = transformJavaStringLiteralExpression(javaStringLiteralExpression)
    fun transformJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier) = transformAccessModifier(javaAccessModifier, null)
    override fun transformJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: Nothing?) = transformJavaAccessModifier(javaAccessModifier)
}
