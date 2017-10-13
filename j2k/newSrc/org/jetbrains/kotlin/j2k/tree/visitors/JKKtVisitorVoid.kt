package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKKtVisitorVoid : JKKtVisitor<Unit, Nothing?>, JKVisitorVoid {
    fun visitKtFun(ktFun: JKKtFun) = visitDeclaration(ktFun, null)
    override fun visitKtFun(ktFun: JKKtFun, data: Nothing?) = visitKtFun(ktFun)
    fun visitKtConstructor(ktConstructor: JKKtConstructor) = visitDeclaration(ktConstructor, null)
    override fun visitKtConstructor(ktConstructor: JKKtConstructor, data: Nothing?) = visitKtConstructor(ktConstructor)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) = visitKtConstructor(ktPrimaryConstructor, null)
    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: Nothing?) = visitKtPrimaryConstructor(ktPrimaryConstructor)
    fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) = visitStatement(ktAssignmentStatement, null)
    override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: Nothing?) = visitKtAssignmentStatement(ktAssignmentStatement)
    fun visitKtCall(ktCall: JKKtCall) = visitCall(ktCall, null)
    override fun visitKtCall(ktCall: JKKtCall, data: Nothing?) = visitKtCall(ktCall)
}
