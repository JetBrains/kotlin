/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.KmClass
import kotlin.metadata.jvm.KotlinClassMetadata

internal fun Class<*>.getMetadata(): Metadata {
    return getAnnotation(Metadata::class.java)
}

internal fun Metadata.readAsKmClass(): KmClass {
    val clazz = KotlinClassMetadata.readStrict(this) as? KotlinClassMetadata.Class
    return clazz?.kmClass ?: error("Not a KotlinClassMetadata.Class: $clazz")
}

internal fun Class<*>.readMetadataAsKmClass(): KmClass = getMetadata().readAsKmClass()

internal fun Class<*>.readMetadataAsClass(): KotlinClassMetadata.Class = getMetadata().readMetadataAsClass()

internal fun Metadata.readMetadataAsClass(): KotlinClassMetadata.Class = KotlinClassMetadata.readStrict(this) as KotlinClassMetadata.Class
