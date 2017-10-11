package org.jetbrains.kotlin.j2k.tree

interface JKJavaKtVisitorVoid : JKJavaKtVisitor<Unit, Nothing?>, JKVisitorVoid {
    fun visitJavaElement(javaElement: JKJavaElement)
    override fun visitJavaElement(javaElement: JKJavaElement, data: Nothing?) = visitJavaElement(javaElement)
    fun visitJavaField(javaField: JKJavaField)
    override fun visitJavaField(javaField: JKJavaField, data: Nothing?) = visitJavaField(javaField)
    fun visitJavaMethod(javaMethod: JKJavaMethod)
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: Nothing?) = visitJavaMethod(javaMethod)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop)
    override fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: Nothing?) = visitJavaForLoop(javaForLoop)
    fun visitKtElement(ktElement: JKKtElement)
    override fun visitKtElement(ktElement: JKKtElement, data: Nothing?) = visitKtElement(ktElement)
    fun visitKtFun(ktFun: JKKtFun)
    override fun visitKtFun(ktFun: JKKtFun, data: Nothing?) = visitKtFun(ktFun)
    fun visitKtConstructor(ktConstructor: JKKtConstructor)
    override fun visitKtConstructor(ktConstructor: JKKtConstructor, data: Nothing?) = visitKtConstructor(ktConstructor)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor)
    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: Nothing?) = visitKtPrimaryConstructor(ktPrimaryConstructor)
}
