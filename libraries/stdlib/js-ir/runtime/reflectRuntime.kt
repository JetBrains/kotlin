/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.reflect.KProperty

internal fun getPropertyCallableRef(
    name: String,
    paramCount: Int,
    superType: dynamic,
    getter: dynamic,
    setter: dynamic
): KProperty<*> {
    getter.get = getter
    getter.set = setter
    getter.callableName = name
    return getPropertyRefClass(
        getter,
        getKPropMetadata(paramCount, setter),
        getInterfaceMaskFor(getter, superType)
    ).unsafeCast<KProperty<*>>()
}

internal fun getLocalDelegateReference(name: String, superType: dynamic, mutable: Boolean, lambda: dynamic): KProperty<*> {
    return getPropertyCallableRef(name, 0, superType, lambda, if (mutable) lambda else null)
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
    return classMeta(VOID, VOID, VOID, VOID)
}

private val propertyRefClassMetadataCache: Array<Array<dynamic>> = arrayOf<Array<dynamic>>(
    //                 immutable     ,     mutable
    arrayOf<dynamic>(metadataObject(), metadataObject()), // 0
    arrayOf<dynamic>(metadataObject(), metadataObject()), // 1
    arrayOf<dynamic>(metadataObject(), metadataObject())  // 2
)

