/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.jetbrains.kotlin.abicmp.reports.SyntheticClassMetadataReport

abstract class SyntheticClassMetadataPropertyChecker(name: String) :
    PropertyChecker<String, KotlinClassMetadata.SyntheticClass>("class.metadata.$name"), SyntheticClassMetadataChecker {

    override fun check(
        metadata1: KotlinClassMetadata.SyntheticClass,
        metadata2: KotlinClassMetadata.SyntheticClass,
        report: SyntheticClassMetadataReport,
    ) {
        val value1 = getProperty(metadata1)
        val value2 = getProperty(metadata2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(NamedDiffEntry(name, value1, value2))
        }
    }
}

fun syntheticClassMetadataPropertyChecker(name: String, propertyGetter: (KotlinClassMetadata.SyntheticClass) -> String) =
    object : SyntheticClassMetadataPropertyChecker(name) {
        override fun getProperty(node: KotlinClassMetadata.SyntheticClass) = propertyGetter(node)
    }
