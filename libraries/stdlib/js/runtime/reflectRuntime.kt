/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.internal.throwUnsupportedOperationException
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
