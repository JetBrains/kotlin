package org.jetbrains.kotlin.j2k.tree

interface JKJavaVisitorVoid : JKJavaVisitor<Unit, Nothing?>, JKVisitorVoid {
    fun visitJavaElement(javaElement: JKJavaElement)
    override fun visitJavaElement(javaElement: JKJavaElement, data: Nothing?) = visitJavaElement(javaElement)
    fun visitJavaField(javaField: JKJavaField)
    override fun visitJavaField(javaField: JKJavaField, data: Nothing?) = visitJavaField(javaField)
    fun visitJavaMethod(javaMethod: JKJavaMethod)
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: Nothing?) = visitJavaMethod(javaMethod)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop)
    override fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: Nothing?) = visitJavaForLoop(javaForLoop)
}
