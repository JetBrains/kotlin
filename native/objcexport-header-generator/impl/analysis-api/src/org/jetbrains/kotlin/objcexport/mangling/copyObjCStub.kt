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

/**
 * See K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStubKt.buildMethodName]
 */
internal fun ObjCMethod.copy(
    mangledSelectors: List<String>,
    mangledParameters: List<String>,
    swiftNameAttribute: String,
    containingStubName: String,
): ObjCMethod {
    val selectorsSize = mangledSelectors.size
    val parametersSize = this.parameters.size
    val methodName = this.name
    require((selectorsSize == parametersSize) || (selectorsSize == 1 && mangledParameters.isEmpty())) {
        "'$containingStubName.$methodName': selectors of doesn't match parameters count: " +
            "selectors($mangledSelectors): $selectorsSize, " +
            "parameters(${this.parameters.joinToString { it.name }}): $parametersSize"
    }
    return ObjCMethod(
        comment = this.comment,
        origin = this.origin,
        isInstanceMethod = this.isInstanceMethod,
        returnType = this.returnType,
        selectors = mangledSelectors,
        parameters = this.parameters,
        attributes = this.attributes.map { attr ->
            if (attr.startsWith("swift_name")) swiftNameAttribute else attr
        },
        extras = this.extras
    )

}