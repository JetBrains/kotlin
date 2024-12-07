/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.checkers.*
import org.jetbrains.kotlin.abicmp.reports.ClassMetadataReport

class ClassMetadataTask(
    private val configuration: CheckerConfiguration,
    private val metadata1: KotlinClassMetadata.Class,
    private val metadata2: KotlinClassMetadata.Class,
    private val report: ClassMetadataReport,
) : Runnable {
    override fun run() {
        for (checker in configuration.enabledClassMetadataCheckers) {
            checker.check(metadata1, metadata2, report)
        }

        checkMetadataMembers(
            metadata1,
            metadata2,
            configuration.enabledConstructorMetadataCheckers,
            report::constructorReport,
            ::loadConstructors
        )

        checkMetadataMembers(
            metadata1,
            metadata2,
            configuration.enabledFunctionMetadataCheckers,
            report::functionReport
        ) { loadFunctions(it.kmClass) }

        checkMetadataMembers(
            metadata1,
            metadata2,
            configuration.enabledPropertyMetadataCheckers,
            report::propertyReport
        ) { loadProperties(it.kmClass) }

        checkMetadataMembers(
            metadata1,
            metadata2,
            configuration.enabledTypeAliasMetadataCheckers,
            report::typeAliasReport
        ) { loadTypeAliases(it.kmClass) }

        checkMetadataMembers(
            metadata1,
            metadata2,
            configuration.enabledPropertyMetadataCheckers,
            report::localDelegatedPropertyReport
        ) { loadLocalDelegatedProperties(it) }
    }
}
