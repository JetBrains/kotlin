package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration

/**
 * Traverses stubs and returns true if [objCErrorType] is used as a return, parameter or property type
 */
internal fun Iterable<ObjCExportStub>.hasErrorTypes(): Boolean {
    return any { stub -> stub.hasErrorTypes() }
}

internal fun ObjCExportStub.hasErrorTypes(): Boolean {
    return when (val stub = this) {
        is ObjCClass -> stub.members.hasErrorTypes()
        is ObjCProperty -> stub.type == objCErrorType
        is ObjCMethod -> {
            if (stub.returnType == objCErrorType) true
            else stub.parameters.any { parameter -> parameter.type == objCErrorType }
        }
        else -> false
    }
}

internal val KaType.isError
    get() = this is KaClassErrorType

internal const val errorClassName = "ERROR"

internal val KtObjCExportSession.errorInterface
    get() = ObjCInterfaceImpl(
        name = errorClassName,
        comment = null,
        origin = null,
        attributes = emptyList(),
        superProtocols = emptyList(),
        members = emptyList(),
        categoryName = null,
        generics = emptyList(),
        superClass = getDefaultSuperClassOrProtocolName().objCName,
        superClassGenerics = emptyList()
    )

internal val objCErrorType = ObjCClassType(errorClassName, extras = objCTypeExtras {
    requiresForwardDeclaration = true
})
