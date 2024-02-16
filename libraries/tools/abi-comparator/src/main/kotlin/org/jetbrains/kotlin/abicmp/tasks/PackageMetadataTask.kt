/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import kotlin.metadata.KmPackage
import org.jetbrains.kotlin.abicmp.checkers.*
import org.jetbrains.kotlin.abicmp.reports.PackageMetadataReport

class PackageMetadataTask(
    private val configuration: CheckerConfiguration,
    private val metadata1: KmPackage,
    private val metadata2: KmPackage,
    private val report: PackageMetadataReport,
) : Runnable {
    override fun run() {
        for (checker in configuration.enabledPackageMetadataCheckers) {
            checker.check(metadata1, metadata2, report)
        }

        checkMetadataMembers(metadata1, metadata2, configuration.enabledFunctionMetadataCheckers, report::functionReport, ::loadFunctions)
        checkMetadataMembers(metadata1, metadata2, configuration.enabledPropertyMetadataCheckers, report::propertyReport, ::loadProperties)
        checkMetadataMembers(
            metadata1,
            metadata2,
            configuration.enabledTypeAliasMetadataCheckers,
            report::typeAliasReport,
            ::loadTypeAliases
        )
        checkMetadataMembers(
            metadata1,
            metadata2,
            configuration.enabledPropertyMetadataCheckers,
            report::localDelegatedPropertyReport,
            ::loadLocalDelegatedProperties
        )
    }

}
