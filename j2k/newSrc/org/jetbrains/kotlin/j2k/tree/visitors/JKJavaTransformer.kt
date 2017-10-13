package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKJavaTransformer<D> : JKTransformer<D> {
    fun transformJavaField(javaField: JKJavaField, data: D): JKElement = transformDeclaration(javaField, data)
    fun transformJavaMethod(javaMethod: JKJavaMethod, data: D): JKElement = transformDeclaration(javaMethod, data)
    fun transformJavaForLoop(javaForLoop: JKJavaForLoop, data: D): JKElement = transformLoop(javaForLoop, data)
    fun transformJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: D): JKElement = transformExpression(javaAssignmentExpression, data)
    fun transformJavaCall(javaCall: JKJavaCall, data: D): JKElement = transformCall(javaCall, data)
    fun transformJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier, data: D): JKElement = transformTypeIdentifier(javaTypeIdentifier, data)
    fun transformJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression, data: D): JKElement = transformLiteralExpression(javaStringLiteralExpression, data)
    fun transformJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: D): JKElement = transformAccessModifier(javaAccessModifier, data)
}
