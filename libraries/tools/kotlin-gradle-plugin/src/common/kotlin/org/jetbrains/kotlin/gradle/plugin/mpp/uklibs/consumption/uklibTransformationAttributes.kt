/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer

internal val AttributeContainer.containsUnzippedUklibAttributes: Boolean
    get() = attributes.getAttribute(uklibStateAttribute) == uklibStateUnzipped

internal val uklibStateAttribute = Attribute.of("uklibState", String::class.java)
internal val uklibStateZipped = "zipped"
internal val uklibStateUnzipped = "unzipped"

internal val uklibTargetAttributeAttribute = Attribute.of("uklibTargetAttribute", String::class.java)
internal val uklibTargetAttributeUnknown = "unknown"