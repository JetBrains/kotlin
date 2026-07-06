/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.internal.throwUnsupportedOperationException
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

@UsedFromCompilerGeneratedCode
internal fun getPropertyCallableRef(
    name: String?,
    paramCount: Int,
    superType: dynamic,
    getter: dynamic,
    setter: dynamic,
    linkageError: String?,
): KProperty<*> {
    getter.get = getter
    getter.set = setter
    getter.callableName = name

    // Since KProperty is not KFunction
    getter[KFunction::class.js.asDynamic().Symbol] = false

    return getPropertyRefClass(
        getter,
        getKPropMetadata(paramCount, setter),
        superType
    ).unsafeCast<KProperty<*>>()
}

@UsedFromCompilerGeneratedCode
internal fun getLocalDelegateReference(name: String, superType: dynamic, mutable: Boolean): KProperty<*> {
    // getPropertyCallableRef will mutate the lambda, so it's important that the lambda is not transformed into a global function.
    val lambda = @JsNoLifting { throwUnsupportedOperationException("Not supported for local property reference.") }
    return getPropertyCallableRef(name, 0, superType, lambda, if (mutable) lambda else null, VOID)
}

private fun getPropertyRefClass(obj: Ctor, metadata: Metadata, superType: Ctor): dynamic {
    obj.`$metadata$` = metadata
    obj.constructor = obj

    val symbol = superType.Symbol
    if (symbol != null) {
        obj.asDynamic()[symbol] = true
    }
    js("Object.assign(obj, superType.prototype)")
    return obj;
}

@Suppress("UNUSED_PARAMETER")
private fun getKPropMetadata(paramCount: Int, setter: Any?): dynamic {
    return propertyRefClassMetadataCache[paramCount][if (setter == null) 0 else 1]
}

private fun metadataObject(): Metadata {
    return createMetadata(METADATA_KIND_CLASS, VOID, VOID, VOID, VOID, VOID)
}

private val propertyRefClassMetadataCache: Array<Array<dynamic>> = arrayOf<Array<dynamic>>(
    //                 immutable     ,     mutable
    arrayOf<dynamic>(metadataObject(), metadataObject()), // 0
    arrayOf<dynamic>(metadataObject(), metadataObject()), // 1
    arrayOf<dynamic>(metadataObject(), metadataObject())  // 2
)

@OptIn(JsIntrinsic::class)
@UsedFromCompilerGeneratedCode
internal fun constructCallableReference(
    callable: dynamic,
    arity: Int,
    minimalArity: Int,
    flags: dynamic,
    signatureId: Any?,
    name: String?,
    bounds: Array<Any>?
): dynamic {
    callable.callableName = name
    callable.`$flags` = flags
    callable.`$id` = signatureId
    callable.`$bound` = bounds
    callable.`$minimalArity` = minimalArity

    // We also use `constructCallableReference` for setting $arity of suspend lambdas
    // (while they are not implementing KFunction)
    // So that, to not accidentally make them KFunction we check if signatureId is provided
    val isKFunction = signatureId !== VOID

    if (isKFunction) {
        val kFunctionClass = KFunction::class.js
        callable[kFunctionClass.asDynamic().Symbol] = true
        js("Object.assign(callable, kFunctionClass.prototype)")
    }

    // It's either a suspend lambda or a suspend function KFunction
    if (!isKFunction || jsBitAnd(flags, 1) === 1) {
        // Extracting continuation from the arity
        callable.`$arity` = arity + 1
        callable.`$suspendArity` = arity
    } else {
        callable.`$arity` = arity
    }

    return callable
}
