/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmTypeAlias
import org.jetbrains.kotlin.abicmp.reports.MetadataPropertyReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry

interface GenericMetadataChecker<T> : Checker {
    fun check(metadata1: T, metadata2: T, report: MetadataPropertyReport)
}

abstract class GenericMetadataPropertyChecker<T>(name: String) :
    PropertyChecker<String, T>("class.metadata.$name"),
    GenericMetadataChecker<T> {

    override fun check(metadata1: T, metadata2: T, report: MetadataPropertyReport) {
        val value1 = getProperty(metadata1)
        val value2 = getProperty(metadata2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(NamedDiffEntry(name, value1, value2))
        }
    }
}

fun constructorMetadataPropertyChecker(name: String, propertyGetter: (KmConstructor) -> String) =
    object : GenericMetadataPropertyChecker<KmConstructor>("constructor.$name") {
        override fun getProperty(node: KmConstructor) = propertyGetter(node)
    }

fun functionMetadataPropertyChecker(name: String, propertyGetter: (KmFunction) -> String) =
    object : GenericMetadataPropertyChecker<KmFunction>("function.$name") {
        override fun getProperty(node: KmFunction) = propertyGetter(node)
    }

fun typeAliasMetadataPropertyChecker(name: String, propertyGetter: (KmTypeAlias) -> String) =
    object : GenericMetadataPropertyChecker<KmTypeAlias>("typeAlias.$name") {
        override fun getProperty(node: KmTypeAlias) = propertyGetter(node)
    }

fun propertyMetadataPropertyChecker(name: String, propertyGetter: (KmProperty) -> String) =
    object : GenericMetadataPropertyChecker<KmProperty>("property.$name") {
        override fun getProperty(node: KmProperty) = propertyGetter(node)
    }
