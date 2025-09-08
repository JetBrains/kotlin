/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.internal.throwIrLinkageError
import kotlin.internal.throwUnsupportedOperationException
import kotlin.reflect.KProperty

@UsedFromCompilerGeneratedCode
internal fun throwLinkageErrorInCallableName(function: dynamic, linkageError: String) {
    defineProp(
        function,
        name = "callableName",
        getter = { throwIrLinkageError(linkageError) },
        setter = VOID,
        enumerable = true,
    )
}

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
    if (linkageError != null) {
        throwLinkageErrorInCallableName(getter, linkageError)
    } else {
        getter.callableName = name
    }
    return getPropertyRefClass(
        getter,
        getKPropMetadata(paramCount, setter),
        getInterfaceMaskFor(getter, superType)
    ).unsafeCast<KProperty<*>>()
}

@UsedFromCompilerGeneratedCode
internal fun getLocalDelegateReference(name: String, superType: dynamic, mutable: Boolean): KProperty<*> {
    // getPropertyCallableRef will mutate the lambda, so it's important that the lambda is not transformed into a global function.
    val lambda = @JsNoLifting { throwUnsupportedOperationException("Not supported for local property reference.") }
    return getPropertyCallableRef(name, 0, superType, lambda, if (mutable) lambda else null, VOID)
}

private fun getPropertyRefClass(obj: Ctor, metadata: Metadata, imask: BitMask): dynamic {
    obj.`$metadata$` = metadata
    obj.constructor = obj
    obj.`$imask$` = imask
    return obj;
}

private fun getInterfaceMaskFor(obj: Ctor, superType: dynamic): BitMask =
    obj.`$imask$` ?: implement(arrayOf(superType))

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
