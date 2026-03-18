/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import java.io.Serializable

@JvmInline
internal value class SwiftPMDependencyIdentifier(val identifier: String) : Serializable

internal data class TransitiveSwiftPMDependencies(
    val metadataByDependencyIdentifier: Map<SwiftPMDependencyIdentifier, SwiftPMImportMetadata>
) : Serializable