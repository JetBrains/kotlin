@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.annotationInfos
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.objcexport.Predefined.anyMethodSelectors
import org.jetbrains.kotlin.objcexport.Predefined.anyMethodSwiftNames
import org.jetbrains.kotlin.objcexport.Predefined.objCReservedNameMethodSelectors
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.objcexport.extras.objCExportStubExtras
import org.jetbrains.kotlin.objcexport.extras.throwsAnnotationClassIds

internal val KtCallableSymbol.isConstructor: Boolean
    get() = this is KtConstructorSymbol

context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.translateToObjCMethod(): ObjCMethod? {
    if (!isVisibleInObjC()) return null
    if (isFakeOverride) return null
    if (this is KtFunctionSymbol && isClone) return null
    return buildObjCMethod()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildMethod]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtFunctionLikeSymbol.buildObjCMethod(
    unavailable: Boolean = false,
): ObjCMethod {

    val bridge = if (this is KtFunctionSymbol) {
        /**
         * Unlike constructor, a function can have base return type.
         * So in case of function we need to call [getFunctionMethodBridge] on [baseMethod]
         */
        baseMethod.getFunctionMethodBridge()
    } else {
        this.getFunctionMethodBridge()
    }

    val returnType: ObjCType = mapReturnType(bridge.returnBridge)
    val parameters = translateToObjCParameters(bridge)
    val selector = getSelector(bridge)
    val selectors = splitSelector(selector)
    val swiftName = getSwiftName(bridge)
    val attributes = mutableListOf<String>()
    val returnBridge = bridge.returnBridge
    val comment = translateToObjCComment(bridge, parameters)
    val throws = definedThrows.map { it }.toList()

    attributes += getSwiftPrivateAttribute() ?: swiftNameAttribute(swiftName)

    if (returnBridge is MethodBridge.ReturnValue.WithError.ZeroForError && returnBridge.successMayBeZero) {
        // Method may return zero on success, but
        // standard Objective-C convention doesn't suppose this happening.
        // Add non-standard convention hint for Swift:
        attributes += "swift_error(nonnull_error)" // Means "failure <=> (error != nil)".
    }

    if (this.isConstructor && !isArrayConstructor) { // TODO: check methodBridge instead.
        attributes += "objc_designated_initializer"
    }

    if (unavailable) {
        attributes += "unavailable"
    } else {
        /**
         * Implement and use [org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver]
         */
        //attributes.addIfNotNull(getDeprecationAttribute(method))
    }

    val isMethodInstance = if (isExtensionOfMappedObjCType) false else bridge.isInstance

    return ObjCMethod(
        comment = comment,
        origin = getObjCExportStubOrigin(),
        isInstanceMethod = isMethodInstance,
        returnType = returnType,
        selectors = selectors,
        parameters = parameters,
        attributes = attributes,
        extras = objCExportStubExtras {
            throwsAnnotationClassIds = throws
        }
    )
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerKt.toValidObjCSwiftIdentifier]
 */
internal fun String.toValidObjCSwiftIdentifier(): String {
    if (this.isEmpty()) return "__"

    return this.replace('$', '_') // TODO: handle more special characters.
        .let { if (it.first().isDigit()) "_$it" else it }
        .let { if (it == "_") "__" else it }
}

internal fun KtCallableSymbol.getSwiftPrivateAttribute(): String? =
    if (isRefinedInSwift()) "swift_private" else null

internal fun KtCallableSymbol.isRefinedInSwift(): Boolean = when {
    // Note: the front-end checker requires all overridden descriptors to be either refined or not refined.
    //overriddenDescriptors.isNotEmpty() -> overriddenDescriptors.first().isRefinedInSwift() //TODO: implement isRefinedInSwift
    else -> annotationInfos.any { annotation ->
        annotation.classId?.asSingleFqName() == KonanFqNames.refinesInSwift
    }
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtFunctionLikeSymbol.getSwiftName(methodBridge: MethodBridge): String {
    //assert(mapper.isBaseMethod(method)) //TODO: implement isBaseMethod
    if (this is KtNamedSymbol) {
        anyMethodSwiftNames[name]?.let { return it }
    }

    val parameters = methodBridge.valueParametersAssociated(this)
    val method = this

    val sb = StringBuilder().apply {
        append(getMangledName(forSwift = true))
        append("(")

        parameters@ for ((bridge, parameter: KtObjCParameterData?) in parameters) {
            val label = when (bridge) {
                is MethodBridgeValueParameter.Mapped -> when {
                    parameter?.isReceiver == true -> "_"
                    method is KtPropertySetterSymbol -> when (parameters.size) {
                        1 -> "_"
                        else -> "value"
                    }
                    else -> {
                        if (parameter == null) continue@parameters
                        else if (parameter.isReceiver) "_" else parameter.name
                    }
                }
                MethodBridgeValueParameter.ErrorOutParameter -> continue@parameters
                is MethodBridgeValueParameter.SuspendCompletion -> "completionHandler"
            }

            append(label)
            append(":")
        }

        append(")")
    }

    return sb.toString()
}


internal object Predefined {
    val anyMethodSelectors = mapOf(
        "hashCode" to "hash",
        "toString" to "description",
        "equals" to "isEqual:"
    ).mapKeys { Name.identifier(it.key) }

    val anyMethodSwiftNames = mapOf(
        "hashCode" to "hash()",
        "toString" to "description()",
        "equals" to "isEqual(_:)"
    ).mapKeys { Name.identifier(it.key) }

    /**
     * [objCReservedNameMethodSelectors] map keys represent name of methods contained in Objective-C's `NSObject` class.
     * These function names are considered reserved since using them in generated headers will result
     * in naming collision with functions from Objective-C's `NSObject` class.
     *
     * [objCReservedNameMethodSelectors] map values represent the mangled function names
     * that should be used
     * when generating Objective-C headers based on the Kotlin functions
     * whose name uses a reserved method name.
     *
     * To avoid function naming collision,
     * generated function names are mangled by appending `_` character at the end of the generated function name.
     *
     * See KT-68051
     * See [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.Mapping.reserved]
     */
    val objCReservedNameMethodSelectors = mapOf(
        "retain" to "retain_",
        "release" to "release_",
        "autorelease" to "autorelease_",
        "class" to "class_",
        "superclass" to "superclass_",
        "hash" to "hash_"
    ).mapKeys { Name.identifier(it.key) }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.splitSelector]
 */
private fun splitSelector(selector: String): List<String> {
    return if (!selector.endsWith(":")) {
        listOf(selector)
    } else {
        selector.trimEnd(':').split(':').map { "$it:" }
    }
}


/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getSelector]
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.getSelector(methodBridge: MethodBridge): String {

    if (this is KtNamedSymbol) {
        val name = this.name

        anyMethodSelectors[name]?.let { return it }
        objCReservedNameMethodSelectors[name]?.let { return it }
    }

    val parameters = methodBridge.valueParametersAssociated(this)
    val method = this
    val sb = StringBuilder()

    sb.append(method.getMangledName(forSwift = false))

    parameters.forEachIndexed { index, (bridge, parameter) ->
        val name = when (bridge) {

            is MethodBridgeValueParameter.Mapped -> when {
                parameter?.isReceiver == true -> ""
                method is KtPropertySetterSymbol -> when (parameters.size) {
                    1 -> ""
                    else -> "value"
                }
                else -> {
                    if (parameter == null) return@forEachIndexed
                    else if (parameter.isReceiver) "" else parameter.name.toString()
                }
            }
            MethodBridgeValueParameter.ErrorOutParameter -> "error"
            is MethodBridgeValueParameter.SuspendCompletion -> "completionHandler"
        }

        if (index == 0) {
            sb.append(
                when {
                    bridge is MethodBridgeValueParameter.ErrorOutParameter -> "AndReturn"
                    bridge is MethodBridgeValueParameter.SuspendCompletion -> "With"
                    method.isConstructor -> "With"
                    else -> ""
                }
            )
            sb.append(name.replaceFirstChar(Char::uppercaseChar))
        } else {
            sb.append(name)
        }

        sb.append(':')
    }

    return sb.toString()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getMangledName]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun KtFunctionLikeSymbol.getMangledName(forSwift: Boolean): String {
    return if (this.isConstructor) {
        if (isArrayConstructor && !forSwift) "array" else "init"
    } else {
        getObjCFunctionName().name(forSwift).handleSpecialNames("do")
    }
}

private fun String.handleSpecialNames(prefix: String): String {
    val trimmed = this.dropWhile { it == '_' }
    for (family in listOf("alloc", "copy", "mutableCopy", "new", "init")) {
        if (trimmed.startsWithWords(family)) {
            return prefix + this.replaceFirstChar(Char::uppercaseChar)
        }
    }
    return this
}

private fun String.startsWithWords(words: String) = this.startsWith(words) &&
        (this.length == words.length || !this[words.length].isLowerCase())

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReturnType]
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.mapReturnType(returnBridge: MethodBridge.ReturnValue): ObjCType {
    return when (returnBridge) {
        MethodBridge.ReturnValue.Suspend,
        MethodBridge.ReturnValue.Void,
        -> ObjCVoidType
        MethodBridge.ReturnValue.HashCode -> ObjCPrimitiveType.NSUInteger
        is MethodBridge.ReturnValue.Mapped -> returnType.translateToObjCType(returnBridge.bridge)
        MethodBridge.ReturnValue.WithError.Success -> ObjCPrimitiveType.BOOL
        is MethodBridge.ReturnValue.WithError.ZeroForError -> {
            val successReturnType = mapReturnType(returnBridge.successBridge)

            if (!returnBridge.successMayBeZero) {
                check(
                    successReturnType is ObjCNonNullReferenceType
                            || (successReturnType is ObjCPointerType && !successReturnType.nullable)
                ) {
                    "Unexpected return type: $successReturnType in $this"
                }
            }

            successReturnType.makeNullableIfReferenceOrPointer()
        }

        MethodBridge.ReturnValue.Instance.InitResult,
        MethodBridge.ReturnValue.Instance.FactoryResult,
        -> ObjCInstanceType
    }
}