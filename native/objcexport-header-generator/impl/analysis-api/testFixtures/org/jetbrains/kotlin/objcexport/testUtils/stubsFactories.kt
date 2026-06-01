package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.backend.konan.objcexport.*

internal fun objCProperty(
    name: String = "",
    type: ObjCType = ObjCIdType,
    propertyAttributes: List<String> = [],
    declarationAttributes: List<String> = [],
): ObjCProperty {
    return ObjCProperty(
        name = name,
        comment = null,
        origin = null,
        type = type,
        propertyAttributes = propertyAttributes,
        setterName = null,
        getterName = null,
        declarationAttributes = declarationAttributes
    )
}

internal fun objCMethod(
    selector: String,
    returnType: ObjCType = ObjCIdType,
    attributes: List<String> = [],
): ObjCMethod {
    return objCMethod([selector], returnType, attributes)
}

internal fun objCMethod(
    selectors: List<String>,
    returnType: ObjCType = ObjCIdType,
    attributes: List<String> = [],
): ObjCMethod {
    return ObjCMethod(
        comment = null,
        origin = null,
        isInstanceMethod = true,
        returnType = returnType,
        selectors = selectors,
        parameters = [],
        attributes = attributes
    )
}

internal fun objCInitMethod(): ObjCMethod {
    return objCMethod(["init"], ObjCInstanceType, [])
}
