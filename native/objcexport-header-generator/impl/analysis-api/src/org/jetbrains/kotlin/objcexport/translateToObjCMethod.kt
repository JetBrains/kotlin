@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.objcexport

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.annotationInfos
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.objcexport.Predefined.anyMethodSelectors
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getMethodBridge
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.psi.KtFile

internal val KtCallableSymbol.isConstructor: Boolean
    get() = this is KtConstructorSymbol

internal val KtCallableSymbol.isArray: Boolean
    get() = false //TODO: temp k2 workaround

context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionSymbol.translateToObjCMethod(
): ObjCMethod? {
    if (!isVisibleInObjC()) return null
    if (anyMethodSelectors.containsKey(this.name)) return null //temp, find replacement for org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.isReal

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
    val bridge = getMethodBridge()
    val origin: ObjCExportStubOrigin? = null
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

    if (this.isConstructor && !isArray) { // TODO: check methodBridge instead.
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
        origin = origin,
        isInstanceMethod = bridge.isInstance || isConstructor,
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
    getPredefined(this, Predefined.anyMethodSwiftNames)?.let { return it }

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
                    else -> symbol!!.getObjCName().name(true)
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
 * Not implemented [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getPredefined]
 */
private fun <T : Any> getPredefined(method: KtFunctionLikeSymbol, predefinedForAny: Map<Name, T>): T? {
    return null
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.getSelector]
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtFunctionLikeSymbol.getSelector(methodBridge: MethodBridge): String {

    getPredefined(this, Predefined.anyMethodSelectors)?.let { return it }

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
                else -> typeParameterSymbol!!.getObjCName().name(false)
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

context(KtAnalysisSession, KtObjCExportSession)
private fun KtFunctionLikeSymbol.getMangledName(forSwift: Boolean): String {

    if (this.isConstructor) {
        return if (isArray && !forSwift) "array" else "init"
    }

    val candidate = when (this) {
        is KtPropertyGetterSymbol -> {
            this.getObjCFunctionName().name(forSwift)
        }
        is KtPropertySetterSymbol -> {
            this.getObjCFunctionName().name(forSwift)
            //TODO: find replacement for [this.correspondingProperty]
//            "set${
//                this.correspondingProperty.getObjCName().asString(forSwift).replaceFirstChar(kotlin.Char::uppercaseChar)
//            }".toIdentifier()
        }
        else -> {
            this.getObjCFunctionName().name(forSwift)
        }
    }
    return candidate.mangleIfSpecialFamily("do")
}


/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.mangleIfSpecialFamily]
 */
private fun String.mangleIfSpecialFamily(prefix: String): String {
    val trimmed = this.dropWhile { it == '_' }
    for (family in listOf("alloc", "copy", "mutableCopy", "new", "init")) {
        if (trimmed.startsWithWords(family)) {
            // Then method can be detected as having special family by Objective-C compiler.
            // mangle the name:
            return prefix + this.replaceFirstChar(Char::uppercaseChar)
        }
    }

    // TODO: handle clashes with NSObject methods etc.

    return this
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.startsWithWords]
 */
private fun String.startsWithWords(words: String) = this.startsWith(words) &&
    (this.length == words.length || !this[words.length].isLowerCase())

@InternalKotlinNativeApi
fun MethodBridge.valueParametersAssociated(
    function: KtFunctionLikeSymbol,
): List<Pair<MethodBridgeValueParameter, KtTypeParameterSymbol?>> {

    val kotlinParameters = function.typeParameters.iterator()
    if (!kotlinParameters.hasNext()) return emptyList()

    val skipFirstKotlinParameter = when (this.receiver) {
        MethodBridgeReceiver.Static -> false
        MethodBridgeReceiver.Factory, MethodBridgeReceiver.Instance -> true
    }
    if (skipFirstKotlinParameter) {
        kotlinParameters.next()
    }

    return this.valueParameters.map {
        when (it) {
            is MethodBridgeValueParameter.Mapped -> it to kotlinParameters.next()
            is MethodBridgeValueParameter.SuspendCompletion,
            is MethodBridgeValueParameter.ErrorOutParameter,
            -> it to null
        }
    }.also { assert(!kotlinParameters.hasNext()) }
}


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
        is MethodBridge.ReturnValue.Mapped -> returnType.mapType(returnBridge.bridge)
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


/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapType]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun KtType.mapType(typeBridge: TypeBridge): ObjCType {

    //if (!this.isObjCObjectType()) return null //TODO implement isObjCObjectType

    return when (typeBridge) {
        is ReferenceBridge -> this.translateToObjCReferenceType()
        is BlockPointerBridge -> this.translateToObjCFunctionType(typeBridge)
        is ValueTypeBridge -> {
            when {
                isBoolean -> ObjCPrimitiveType.BOOL
                isChar -> ObjCPrimitiveType.int8_t
                isByte -> ObjCPrimitiveType.char
                isShort -> ObjCPrimitiveType.int16_t
                isInt -> ObjCPrimitiveType.int32_t
                isLong -> ObjCPrimitiveType.long_long
                isFloat -> ObjCPrimitiveType.float
                isDouble -> ObjCPrimitiveType.double
                isUByte -> ObjCPrimitiveType.unsigned_char
                isUShort -> ObjCPrimitiveType.unsigned_short
                isUInt -> ObjCPrimitiveType.unsigned_int
                isULong -> ObjCPrimitiveType.unsigned_long_long


                else -> TODO("Handle primitive KtType mapping to ObjCType: $this")
            }
        }
    }
}
