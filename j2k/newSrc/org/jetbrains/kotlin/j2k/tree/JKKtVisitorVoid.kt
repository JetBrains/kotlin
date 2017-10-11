package org.jetbrains.kotlin.j2k.tree

interface JKKtVisitorVoid : JKKtVisitor<Unit, Nothing?>, JKVisitorVoid {
    fun visitKtElement(ktElement: JKKtElement)
    override fun visitKtElement(ktElement: JKKtElement, data: Nothing?) = visitKtElement(ktElement)
    fun visitKtFun(ktFun: JKKtFun)
    override fun visitKtFun(ktFun: JKKtFun, data: Nothing?) = visitKtFun(ktFun)
    fun visitKtConstructor(ktConstructor: JKKtConstructor)
    override fun visitKtConstructor(ktConstructor: JKKtConstructor, data: Nothing?) = visitKtConstructor(ktConstructor)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor)
    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: Nothing?) = visitKtPrimaryConstructor(ktPrimaryConstructor)
}
