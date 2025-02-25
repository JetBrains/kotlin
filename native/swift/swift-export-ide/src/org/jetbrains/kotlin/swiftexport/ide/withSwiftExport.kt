/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.ide

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.utils.SilentUnsupportedDeclarationReporter
import org.jetbrains.kotlin.swiftexport.ide.session.IdeSirSession

public class SwiftExportConfiguration(
    public val moduleForPackagesName: String = "ExportedKotlinPackages",
)

public inline fun <T> KaSession.withSirSession(
    configuration: SwiftExportConfiguration = SwiftExportConfiguration(),
    block: SirSession.() -> T,
): T = IdeSirSession(
    kaModule = useSiteModule,
    moduleForPackageEnums = buildModule { name = configuration.moduleForPackagesName },
    unsupportedDeclarationReporter = SilentUnsupportedDeclarationReporter,
    targetPackageFqName = null,
    platformLibs = setOf() // todo: in ideal world IDE should pass platform libs to the sirSession
).block()
