package org.jetbrains.kotlin.j2k.tree

interface JKJavaKtVisitor<R, D> : JKVisitor<R, D> {
    fun visitJavaElement(javaElement: JKJavaElement, data: D): R
    fun visitJavaField(javaField: JKJavaField, data: D): R
    fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): R
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: D): R
    fun visitKtElement(ktElement: JKKtElement, data: D): R
    fun visitKtFun(ktFun: JKKtFun, data: D): R
    fun visitKtConstructor(ktConstructor: JKKtConstructor, data: D): R
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): R
}
