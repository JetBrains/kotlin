package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKKtTransformer<D> : JKTransformer<D> {
    fun transformKtFun(ktFun: JKKtFun, data: D): JKElement = transformDeclaration(ktFun, data)
    fun transformKtConstructor(ktConstructor: JKKtConstructor, data: D): JKElement = transformDeclaration(ktConstructor, data)
    fun transformKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): JKElement = transformKtConstructor(ktPrimaryConstructor, data)
    fun transformKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: D): JKElement = transformStatement(ktAssignmentStatement, data)
    fun transformKtCall(ktCall: JKKtCall, data: D): JKElement = transformCall(ktCall, data)
}
