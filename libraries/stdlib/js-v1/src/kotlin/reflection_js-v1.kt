/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.reflect.KClass

@PublishedApi
internal fun <T : Annotation> KClass<*>.findAssociatedObject(@Suppress("UNUSED_PARAMETER") annotationClass: KClass<T>): Any? {
    // This API is not supported in js-v1. Return `null` to be source-compatible with js-ir.
    return null
}
