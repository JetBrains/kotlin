package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty


internal fun ObjCProperty.copy(getterPropertyAttribute: String): ObjCProperty {
    return ObjCProperty(
        name = name,
        comment = comment,
        origin = origin,
        type = type,
        propertyAttributes = propertyAttributes + getterPropertyAttribute,
        setterName = setterName,
        getterName = getterName,
        declarationAttributes = declarationAttributes,
        extras = extras
    )
}

internal fun ObjCMethod.copy(selectors: List<String>, parameters: List<String>, swiftNameAttribute: String): ObjCMethod {
    require(selectors.size == this.parameters.size) {
        "selectors count doesn't match parameters count: selectors: ${selectors.size}, parameters: ${parameters.size}"
    }
    return ObjCMethod(
        comment = this.comment,
        origin = this.origin,
        isInstanceMethod = this.isInstanceMethod,
        returnType = this.returnType,
        selectors = selectors,
        parameters = this.parameters,
        attributes = listOf(swiftNameAttribute),
        extras = this.extras
    )
}