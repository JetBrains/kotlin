package org.jetbrains.kotlin.j2k.tree

interface JKKtVisitor<R, D> : JKVisitor<R, D> {
    fun visitKtElement(ktElement: JKKtElement, data: D): R
    fun visitKtFun(ktFun: JKKtFun, data: D): R
    fun visitKtConstructor(ktConstructor: JKKtConstructor, data: D): R
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): R
}
