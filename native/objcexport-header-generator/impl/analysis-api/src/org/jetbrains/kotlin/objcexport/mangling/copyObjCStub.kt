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
    containingStubName: String,
): ObjCMethod {
    val selectorsSize = mangledSelectors.size
    val parametersSize = this.parameters.size
    val methodName = this.name
    require((selectorsSize == parametersSize) || (selectorsSize == 1 && parametersSize == 0)) {
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
            if (attr.startsWith("swift_name")) remangledSwiftName(mangledSelectors, this.attributes) else attr
        },
        extras = this.extras
    )
}

/**
 * Create a swift name based on the selectors and attributes.
 */
internal fun ObjCMethod.remangledSwiftName(mangledSelectors: List<String>, attributes: List<String>): String {
    fun isConstructor(attributes: List<String>) : Boolean {
        return attributes.contains("objc_designated_initializer")
    }

    val swiftName = StringBuilder("swift_name(\"")
    val count = if (mangledSelectors[0].endsWith(":")) mangledSelectors.size else mangledSelectors.size - 1
    if (count == 0) {
        // - (void)bar; -> bar()
        swiftName.append(mangledSelectors[0] + "()")
    } else {
        val selector = mangledSelectors[0]
        val lastUppercaseChar = selector.indexOfLast { it.isUpperCase() }
        if (isConstructor(attributes)) {
            // - (instancetype)initWithA:...; -> init(a:)
            var prefix = selector.take(lastUppercaseChar)
            if (prefix.endsWith("With")) {
                prefix = prefix.dropLast(4)
            }
            swiftName.append(
                prefix
                    + "("
                    + selector.substring(lastUppercaseChar).replaceFirstChar { it.lowercase() })
        } else if (lastUppercaseChar == -1 && selector.endsWith("_:")) {
            //- (Foo *)days_... -> days(__:...)
            //- (Foo *)days__... -> days(___:...)
            val index = selector.indexOf("_")
            swiftName.append(selector.substring(0, index) + "(")
            for (i in index until selector.length) {
                swiftName.append("_")
            }
            swiftName.append(":")
        } else {
            if (lastUppercaseChar == -1) {
                // - (void)bar:; -> bar:(_)
                swiftName.append(selector.dropLast(1) + "(_")
                if (count > 1) {
                    // - (void)bar:...; -> bar:(_:...
                    swiftName.append(":")
                }
            } else {
                // - (void)doBar:...; -> do(bar:...)
                swiftName.append(
                    selector.take(lastUppercaseChar)
                        + "("
                        + selector.substring(lastUppercaseChar).replaceFirstChar { it.lowercase() })
            }
        }
        if (count > 1) {
            for (i in 1 until count) {
                swiftName.append(mangledSelectors[i])
            }
        }
        swiftName.append(")")
    }
    swiftName.append("\")")
    return swiftName.toString()
}
