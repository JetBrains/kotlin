package org.jetbrains.kotlin.j2k.tree

interface JKJavaVisitor<R, D> : JKVisitor<R, D> {
    fun visitJavaField(javaField: JKJavaField, data: D): R = visitDeclaration(javaField, data)
    fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): R = visitDeclaration(javaMethod, data)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: D): R = visitLoop(javaForLoop, data)
}
