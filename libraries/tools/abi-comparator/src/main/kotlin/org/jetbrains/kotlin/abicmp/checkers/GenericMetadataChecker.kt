/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.MetadataPropertyReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmTypeAlias
import kotlin.metadata.jvm.KmModule
import kotlin.metadata.jvm.KmPackageParts
import kotlin.metadata.jvm.UnstableMetadataApi

interface GenericMetadataChecker<T> : Checker {
    fun check(metadata1: T, metadata2: T, report: MetadataPropertyReport)
}

abstract class GenericMetadataPropertyChecker<T>(name: String) :
    PropertyChecker<String, T>(name),
    GenericMetadataChecker<T> {

    override fun check(metadata1: T, metadata2: T, report: MetadataPropertyReport) {
        val value1 = getProperty(metadata1)
        val value2 = getProperty(metadata2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(NamedDiffEntry(name, value1, value2))
        }
    }
}

@OptIn(UnstableMetadataApi::class)
fun moduleMetadataPropertyChecker(name: String, propertyGetter: (KmModule) -> String) =
    object : GenericMetadataPropertyChecker<KmModule>("module.metadata.$name") {
        override fun getProperty(node: KmModule) = propertyGetter(node)
    }

@OptIn(UnstableMetadataApi::class)
fun packagePartsPropertyChecker(name: String, propertyGetter: (KmPackageParts) -> String) =
    object : GenericMetadataPropertyChecker<KmPackageParts>("module.metadata.$name") {
        override fun getProperty(node: KmPackageParts) = propertyGetter(node)
    }

fun constructorMetadataPropertyChecker(name: String, propertyGetter: (KmConstructor) -> String) =
    object : GenericMetadataPropertyChecker<KmConstructor>("class.metadata.constructor.$name") {
        override fun getProperty(node: KmConstructor) = propertyGetter(node)
    }

fun functionMetadataPropertyChecker(name: String, propertyGetter: (KmFunction) -> String) =
    object : GenericMetadataPropertyChecker<KmFunction>("class.metadata.function.$name") {
        override fun getProperty(node: KmFunction) = propertyGetter(node)
    }

fun typeAliasMetadataPropertyChecker(name: String, propertyGetter: (KmTypeAlias) -> String) =
    object : GenericMetadataPropertyChecker<KmTypeAlias>("class.metadata.typeAlias.$name") {
        override fun getProperty(node: KmTypeAlias) = propertyGetter(node)
    }

fun propertyMetadataPropertyChecker(name: String, propertyGetter: (KmProperty) -> String) =
    object : GenericMetadataPropertyChecker<KmProperty>("class.metadata.property.$name") {
        override fun getProperty(node: KmProperty) = propertyGetter(node)
    }
