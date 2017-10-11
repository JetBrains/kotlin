package org.jetbrains.kotlin.j2k.tree

interface JKJavaVisitorVoid : JKJavaVisitor<Unit, Nothing?>, JKVisitorVoid {
    fun visitJavaField(javaField: JKJavaField) = visitDeclaration(javaField, null)
    override fun visitJavaField(javaField: JKJavaField, data: Nothing?) = visitJavaField(javaField)
    fun visitJavaMethod(javaMethod: JKJavaMethod) = visitDeclaration(javaMethod, null)
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: Nothing?) = visitJavaMethod(javaMethod)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop) = visitLoop(javaForLoop, null)
    override fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: Nothing?) = visitJavaForLoop(javaForLoop)
}
