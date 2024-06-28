/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.reports.ClassMetadataReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry

abstract class ClassMetadataPropertyChecker(name: String) : PropertyChecker<String, KotlinClassMetadata.Class>("class.metadata.$name"),
    ClassMetadataChecker {

    override fun check(metadata1: KotlinClassMetadata.Class, metadata2: KotlinClassMetadata.Class, report: ClassMetadataReport) {
        val value1 = getProperty(metadata1)
        val value2 = getProperty(metadata2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(NamedDiffEntry(name, valueToHtml(value1, value2), valueToHtml(value2, value1)))
        }
    }
}

fun classMetadataPropertyChecker(name: String, propertyGetter: (KotlinClassMetadata.Class) -> String) =
    object : ClassMetadataPropertyChecker(name) {
        override fun getProperty(node: KotlinClassMetadata.Class) = propertyGetter(node)
    }
