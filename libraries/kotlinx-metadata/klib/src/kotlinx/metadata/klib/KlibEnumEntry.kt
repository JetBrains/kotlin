/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.KmAnnotation

// TODO: Add to common kotlinx-metadata.
class KlibEnumEntry(
    val name: String,
    var uniqId: UniqId? = null,
    var ordinal: Int? = null,
    val annotations: MutableList<KmAnnotation> = mutableListOf()
)
