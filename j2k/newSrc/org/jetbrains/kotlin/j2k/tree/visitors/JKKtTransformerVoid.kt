package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKKtTransformerVoid : JKKtTransformer<Nothing?>, JKTransformerVoid {
    fun transformKtFun(ktFun: JKKtFun) = transformDeclaration(ktFun, null)
    override fun transformKtFun(ktFun: JKKtFun, data: Nothing?) = transformKtFun(ktFun)
    fun transformKtConstructor(ktConstructor: JKKtConstructor) = transformDeclaration(ktConstructor, null)
    override fun transformKtConstructor(ktConstructor: JKKtConstructor, data: Nothing?) = transformKtConstructor(ktConstructor)
    fun transformKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) = transformKtConstructor(ktPrimaryConstructor, null)
    override fun transformKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: Nothing?) = transformKtPrimaryConstructor(ktPrimaryConstructor)
    fun transformKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) = transformStatement(ktAssignmentStatement, null)
    override fun transformKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: Nothing?) = transformKtAssignmentStatement(ktAssignmentStatement)
    fun transformKtCall(ktCall: JKKtCall) = transformCall(ktCall, null)
    override fun transformKtCall(ktCall: JKKtCall, data: Nothing?) = transformKtCall(ktCall)
}
