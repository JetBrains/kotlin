package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.backend.konan.objcexport.*

context(KtAnalysisSession, KtObjCExportSession)
internal fun collectDependencies(stubs: List<ObjCExportStub>): Collection<ObjCExportStub> {
    val result = mutableMapOf<String, ObjCExportStub>()
    stubs.forEach { stub ->
        collectDependencies(stub, result)
    }

    return result.values
}

context(KtAnalysisSession, KtObjCExportSession)
private fun collectDependencies(stub: ObjCExportStub, result: MutableMap<String, ObjCExportStub>) {
    if (stub is ObjCClass) {
        traverseMembers(stub.members, result)
    }
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtAnalysisSession.traverseMembers(
    members: List<ObjCExportStub>,
    result: MutableMap<String, ObjCExportStub>,
) {
    members.forEach { member ->
        if (member is ObjCProperty) {
            val type = member.type
            collectDependency(type, result)
        } else if (member is ObjCMethod) {
            collectDependency(member.returnType, result)
            member.parameters.forEach { parameter ->
                collectDependency(parameter.type, result)
            }
        }
    }
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtAnalysisSession.collectDependency(
    type: ObjCType,
    result: MutableMap<String, ObjCExportStub>,
) {
    if (type !is ObjCReferenceType) return
    val classId = type.classId ?: return
    val fqName = classId.asSingleFqName().asString()
    if (result.containsKey(fqName)) return

    val symbol = getClassOrObjectSymbolByClassId(classId)
    val isClass = symbol?.classKind == KtClassKind.CLASS
    val isInterface = symbol?.classKind == KtClassKind.INTERFACE
    val stub: ObjCExportStub? = if (isClass) {
        symbol?.translateToObjCClass()
    } else if (isInterface) {
        symbol?.translateToObjCProtocol()
    } else {
        null
    }
    if (stub != null) {
        result[fqName] = stub
        collectDependencies(stub, result)
    }
}