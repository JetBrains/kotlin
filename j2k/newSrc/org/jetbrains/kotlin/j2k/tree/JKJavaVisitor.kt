package org.jetbrains.kotlin.j2k.tree

interface JKJavaVisitor<R, D> : JKVisitor<R, D> {
    fun visitJavaElement(javaElement: JKJavaElement, data: D): R
    fun visitJavaField(javaField: JKJavaField, data: D): R
    fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): R
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: D): R
}
