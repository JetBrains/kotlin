/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer

// Means this is uklib itself of a platform fragment transformed from the uklib
internal val ResolvedArtifactResult.isFromUklib: Boolean
    get() = variant.attributes.containsDecompressedUklibAttributes

internal val AttributeContainer.containsDecompressedUklibAttributes: Boolean
    get() = attributes.getAttribute(uklibStateAttribute) == uklibStateDecompressed

/**
 * These attributes are only used for transforms in the resolvable configurations. They are not used in consumable configurations and are
 * therefore never published.
 */
internal val uklibStateAttribute = Attribute.of("org.jetbrains.kotlin.uklibState", String::class.java)
internal val uklibStateCompressed = "compressed"
internal val uklibStateDecompressed = "decompressed"

internal val uklibViewAttribute = Attribute.of("org.jetbrains.kotlin.uklibView", String::class.java)
internal val uklibViewAttributeWholeUklib = "whole_uklib"