@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.objcexport

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.annotationInfos
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.objcexport.Predefined.anyMethodSelectors
import org.jetbrains.kotlin.objcexport.Predefined.anyMethodSwiftNames
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.psi.KtFile

internal val KtCallableSymbol.isConstructor: Boolean
    get() = this is KtConstructorSymbol

context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionSymbol.translateToObjCMethod(): ObjCMethod? {
    if (!isVisibleInObjC()) return null
    if (isFakeOverride) return null
    if (isClone) return null
    return buildObjCMethod()
}

context(KtAnalysisSession, KtObjCExportSession)
fun KtFileSymbol.getObjCFileClassOrProtocolName(): ObjCExportFileName? {
    val ktFile = this.psi as? KtFile ?: return null
    val name = NameUtils.getPackagePartClassNamePrefix(FileUtil.getNameWithoutExtension(ktFile.name)) + "Kt"
    return name.toIdentifier().getObjCFileName()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildMethod]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtFunctionLikeSymbol.buildObjCMethod(
    unavailable: Boolean = false,
): ObjCMethod {
    val bridge = getFunctionMethodBridge()
    val returnType: ObjCType = mapReturnType(bridge.returnBridge)
    val parameters = translateToObjCParameters(bridge)
    val selector = getSelector(bridge)
    val selectors: List<String> = splitSelector(selector)
    val swiftName = getSwiftName(bridge)
    val attributes = mutableListOf<String>()
    val returnBridge = bridge.returnBridge
    val comment = this.translateToObjCComment(bridge, parameters)

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

    return ObjCMethod(
        comment = comment,
        origin = getObjCExportStubOrigin(),
        isInstanceMethod = bridge.isInstance,
        returnType = returnType,
        selectors = selectors,
        parameters = parameters,
        attributes = attributes
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

        parameters@ for ((bridge, symbol) in parameters) {
            val label = when (bridge) {
                is MethodBridgeValueParameter.Mapped -> when {
                    //it is ReceiverParameterDescriptor -> it.getObjCName().asIdentifier(true) { "_" }
                    method is KtPropertySetterSymbol -> when (parameters.size) {
                        1 -> "_"
                        else -> "value"
                    }
                    else -> symbol!!.name
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
        anyMethodSelectors[this.name]?.let { return it }
    }

    val parameters = methodBridge.valueParametersAssociated(this)

    val method = this

    val sb = StringBuilder()

    sb.append(method.getMangledName(forSwift = false))

    parameters.forEachIndexed { index, (bridge, typeParameterSymbol) ->
        val name = when (bridge) {

            is MethodBridgeValueParameter.Mapped -> when {
                method is KtPropertySetterSymbol -> when (parameters.size) {
                    1 -> ""
                    else -> "value"
                }
                else -> {
                    typeParameterSymbol!!.name.toString()
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

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.MethodBrideExtensionsKt.valueParametersAssociated]
 */
@InternalKotlinNativeApi
fun MethodBridge.valueParametersAssociated(
    function: KtFunctionLikeSymbol,
): List<Pair<MethodBridgeValueParameter, KtValueParameterSymbol?>> {
    val allParameters = function.valueParameters
    if (allParameters.isEmpty()) return emptyList()

    return this.valueParameters.mapIndexed { index, valueParameterBridge ->
        when (valueParameterBridge) {
            is MethodBridgeValueParameter.Mapped -> valueParameterBridge to allParameters[index]

            is MethodBridgeValueParameter.SuspendCompletion,
            is MethodBridgeValueParameter.ErrorOutParameter,
            -> valueParameterBridge to null
        }
    }
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
