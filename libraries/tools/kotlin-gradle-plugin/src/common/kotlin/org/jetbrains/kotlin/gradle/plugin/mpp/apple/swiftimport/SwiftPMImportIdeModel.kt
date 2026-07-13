/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import java.io.File

internal sealed class SwiftPMDependencyForIde

internal data class LocalSwiftPMDependencyForIde(val absolutePath: File) : SwiftPMDependencyForIde()

internal data class RemoteSwiftPMDependencyForIde(val url: String) : SwiftPMDependencyForIde()

internal data class DeclaredSwiftPMDependencies(
    val dependencies: List<SwiftPMDependencyForIde>,
    val checkoutPath: File,
    val swiftPackageResolveTaskPath: String,
)

@Suppress("unused")
internal data class SwiftPMImportIdeModel(
    val hasSwiftPMDependencies: Boolean,
    val integrateLinkagePackageTaskPath: String,
    val magicPackageName: String,
    val declaredSwiftPMDependencies: DeclaredSwiftPMDependencies?,
)
