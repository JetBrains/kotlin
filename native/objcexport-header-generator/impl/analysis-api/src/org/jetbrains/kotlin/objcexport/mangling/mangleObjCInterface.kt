package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal fun ObjCExportContext.mangleObjCInterface(objCInterface: ObjCInterface, name: String): ObjCInterface {
    return ObjCInterfaceImpl(
        name = name,
        comment = objCInterface.comment,
        origin = objCInterface.origin,
        attributes = objCInterface.attributes,
        superProtocols = objCInterface.superProtocols,
        members = mangleObjCProperties(mangleObjCMethods(objCInterface.members)),
        categoryName = objCInterface.categoryName,
        generics = mangleObjCParametersGenerics(objCInterface.generics),
        superClass = objCInterface.superClass,
        superClassGenerics = mangleSuperClassGenerics(objCInterface.superClassGenerics),
        extras = objCInterface.extras
    )
}

private fun ObjCExportContext.mangleSuperClassGenerics(types: List<ObjCNonNullReferenceType>): List<ObjCNonNullReferenceType> {
    return types.map { type ->
        if (type is ObjCGenericTypeParameterUsage) mangleObjCGenericType(type) else type
    }
}

internal fun ObjCExportContext.mangleObjCParametersGenerics(declarations: List<ObjCGenericTypeDeclaration>): List<ObjCGenericTypeDeclaration> {
    return declarations.map { declaration ->
        mangleObjCDeclaration(declaration)
    }
}

private fun ObjCExportContext.mangleObjCDeclaration(declaration: ObjCGenericTypeDeclaration): ObjCGenericTypeDeclaration {
    if (isReservedTypeParameterName(declaration.typeName)) {
        if (declaration is ObjCGenericTypeRawDeclaration)
            return ObjCGenericTypeRawDeclaration(declaration.typeName + "_", declaration.variance)
        else if (declaration is ObjCGenericTypeParameterDeclaration)
            return ObjCGenericTypeParameterDeclaration(declaration.typeName + "_", declaration.variance)
    }
    return declaration
}