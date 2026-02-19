/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.reports.MultiFileClassPartMetadataReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry

abstract class MultiFileClassPartMetadataPropertyChecker(name: String) :
    PropertyChecker<String, KotlinClassMetadata.MultiFileClassPart>("class.metadata.$name"), MultiFileClassPartMetadataChecker {

    override fun check(
        metadata1: KotlinClassMetadata.MultiFileClassPart,
        metadata2: KotlinClassMetadata.MultiFileClassPart,
        report: MultiFileClassPartMetadataReport,
    ) {
        val value1 = getProperty(metadata1)
        val value2 = getProperty(metadata2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(NamedDiffEntry(name, value1, value2))
        }
    }
}

fun multiFileClassPartMetadataPropertyChecker(name: String, propertyGetter: (KotlinClassMetadata.MultiFileClassPart) -> String) =
    object : MultiFileClassPartMetadataPropertyChecker(name) {
        override fun getProperty(node: KotlinClassMetadata.MultiFileClassPart) = propertyGetter(node)
    }
