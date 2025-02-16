/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.config

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.swiftexport.standalone.UnsupportedDeclarationReporterKind
import org.jetbrains.kotlin.swiftexport.standalone.utils.rootPackageToFqn

/**
 * @param experimentalFeatures currently used to pass custom settings in between KGP and SwiftExport.
 * We plan to improve experiments support and replace the map. See KT-75191 for details.
 */
public data class SwiftModuleConfig(
    val bridgeModuleName: String = DEFAULT_BRIDGE_MODULE_NAME,
    val rootPackage: String? = null,
    val unsupportedDeclarationReporterKind: UnsupportedDeclarationReporterKind = UnsupportedDeclarationReporterKind.Silent,
    val experimentalFeatures: Map<String, String> = emptyMap(),
) {

    val targetPackageFqName: FqName? = rootPackage?.rootPackageToFqn()
    val unsupportedDeclarationReporter: UnsupportedDeclarationReporter = unsupportedDeclarationReporterKind.toReporter()

    public companion object {
        public const val ROOT_PACKAGE: String = "packageRoot"
        public const val DEFAULT_BRIDGE_MODULE_NAME: String = "KotlinBridges"
        public const val UNSUPPORTED_DECLARATIONS_REPORTER_KIND: String = "unsupportedDeclarationsReporterKind"
    }
}