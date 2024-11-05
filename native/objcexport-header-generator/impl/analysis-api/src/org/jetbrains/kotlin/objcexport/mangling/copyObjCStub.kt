package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty


internal fun ObjCProperty.copy(
    name: String,
    propertyAttributes: String?,
    declarationAttributes: List<String>?,
): ObjCProperty {
    return ObjCProperty(
        name = name,
        comment = comment,
        origin = origin,
        type = type,
        propertyAttributes = if (propertyAttributes == null) this.propertyAttributes else this.propertyAttributes + propertyAttributes,
        setterName = setterName,
        getterName = getterName,
        declarationAttributes = declarationAttributes ?: this.declarationAttributes,
        extras = extras
    )
}

internal fun ObjCMethod.copy(
    selectors: List<String>,
    parameters: List<String>,
    swiftNameAttribute: String,
): ObjCMethod {
    require((selectors.size == this.parameters.size) || (selectors.size == 1 && parameters.isEmpty())) {
        "selectors count doesn't match parameters count: " +
            "selectors($selectors): ${selectors.size}, " +
            "parameters($parameters): ${parameters.size}"
    }
    return ObjCMethod(
        comment = this.comment,
        origin = this.origin,
        isInstanceMethod = this.isInstanceMethod,
        returnType = this.returnType,
        selectors = selectors,
        parameters = this.parameters,
        attributes = this.attributes.map { attr ->
            if (attr.startsWith("swift_name")) swiftNameAttribute else attr
        },
        extras = this.extras
    )
}