/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * The Kotlin type representation for using in the scripting API
 */
class KotlinType private constructor(
    val typeName: String,
    @Transient val fromClass: KClass<*>? = null
    // TODO: copy properties from KType
) : Serializable {
    /**
     * Constructs KotlinType from fully-qualified [qualifiedTypeName] in a dot-separated form, e.g. "org.acme.Outer.Inner"
     */
    constructor(qualifiedTypeName: String) : this(qualifiedTypeName, null)

    /**
     * Constructs KotlinType from reflected [kclass]
     */
    constructor(kclass: KClass<*>) : this(kclass.qualifiedName!!, kclass)

    // TODO: implement other approach for non-class types
    /**
     * Constructs KotlinType from reflected [ktype]
     */
    constructor(type: KType) : this(type.classifier as KClass<*>)
}
