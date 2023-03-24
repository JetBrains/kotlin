/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassMetadata

fun Class<*>.readMetadata(): Metadata {
    return getAnnotation(Metadata::class.java)
}

fun KotlinClassMetadata.asKmClass(): KmClass = (this as? KotlinClassMetadata.Class)?.toKmClass()
    ?: error("Not a KotlinClassMetadata.Class: $this")

fun Class<*>.readKmClass() = KotlinClassMetadata.read(this.readMetadata())?.asKmClass()!!
