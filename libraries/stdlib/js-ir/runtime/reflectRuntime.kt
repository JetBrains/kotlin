/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.reflect.KProperty

internal fun getPropertyCallableRef(name: String, paramCount: Int, type: dynamic, getter: dynamic, setter: dynamic): KProperty<*> {
    getter.get = getter
    getter.set = setter
    getter.callableName = name
    return getPropertyRefClass(getter, getKPropMetadata(paramCount, setter, type)).unsafeCast<KProperty<*>>()
}

internal fun getLocalDelegateReference(name: String, type: dynamic, mutable: Boolean, lambda: dynamic): KProperty<*> {
    return getPropertyCallableRef(name, 0, type, lambda, if (mutable) lambda else null)
}

private fun getPropertyRefClass(obj: Ctor, metadata: Metadata): dynamic {
    obj.`$metadata$` = metadata;
    obj.constructor = obj;
    return obj;
}

private fun getKPropMetadata(paramCount: Int, setter: Any?, type: dynamic): dynamic {
    val mdata: Metadata = propertyRefClassMetadataCache[paramCount][if (setter == null) 0 else 1]

    if (mdata.interfaces.size == 0) {
        mdata.interfaces.asDynamic().push(type)

        if (mdata.interfacesCache == null) {
            mdata.interfacesCache = generateInterfaceCache()
        } else {
            mdata.interfacesCache!!.isComplete = false
        }

        mdata.interfacesCache!!.extendCacheWithSingle(type)
    }

    return mdata
}

private fun metadataObject(): Metadata {
    val undef = js("undefined")
    return classMeta(undef, undef, undef, undef, undef, undef)
}

private val propertyRefClassMetadataCache: Array<Array<dynamic>> = arrayOf<Array<dynamic>>(
    //                 immutable     ,     mutable
    arrayOf<dynamic>(metadataObject(), metadataObject()), // 0
    arrayOf<dynamic>(metadataObject(), metadataObject()), // 1
    arrayOf<dynamic>(metadataObject(), metadataObject())  // 2
)

