/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.reflect.KType

class KotlinType(
    val typeName: String,
    val fromClass: KClass<*>? = null
    // TODO: copy properties from KType
) {
    // TODO: implement other approach for non-class types
    constructor(type: KType) : this((type.classifier as KClass<*>).qualifiedName!!, type.classifier as KClass<*>)

    constructor(kclass: KClass<*>) : this(kclass.qualifiedName!!, kclass)
}
