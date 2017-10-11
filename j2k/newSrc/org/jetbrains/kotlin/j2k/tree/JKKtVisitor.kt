package org.jetbrains.kotlin.j2k.tree

interface JKKtVisitor<R, D> : JKVisitor<R, D> {
    fun visitKtFun(ktFun: JKKtFun, data: D): R = visitDeclaration(ktFun, data)
    fun visitKtConstructor(ktConstructor: JKKtConstructor, data: D): R = visitDeclaration(ktConstructor, data)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): R = visitKtConstructor(ktPrimaryConstructor, data)
}
