/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.*
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.localDelegatedProperties
import kotlin.metadata.jvm.signature
import org.jetbrains.kotlin.abicmp.reports.MetadataPropertyReport
import org.jetbrains.kotlin.abicmp.tasks.GenericMetadataTask

fun loadProperties(container: KmDeclarationContainer) = container.properties.associateBy { it.getterSignature?.toString() ?: it.name }

fun loadConstructors(clazz: KotlinClassMetadata.Class) = clazz.kmClass.constructors.associateBy { it.signature.toString() }

fun loadFunctions(container: KmDeclarationContainer) = container.functions.associateBy { (it.signature ?: it.name).toString() }

fun loadTypeAliases(container: KmDeclarationContainer) = container.typeAliases.associateBy { it.name }

fun loadLocalDelegatedProperties(kmPackage: KmPackage) =
    kmPackage.localDelegatedProperties.associateBy { it.getterSignature?.toString() ?: it.name }

fun loadLocalDelegatedProperties(clazz: KotlinClassMetadata.Class) =
    clazz.kmClass.localDelegatedProperties.associateBy { it.getterSignature?.toString() ?: it.name }

inline fun <R, T> checkMetadataMembers(
    metadata1: R,
    metadata2: R,
    checkers: List<GenericMetadataChecker<T>>,
    reportBuilder: (String) -> MetadataPropertyReport,
    membersGetter: (R) -> Map<String, T>,
) {
    val membersMetadata1 = membersGetter(metadata1)
    val membersMetadata2 = membersGetter(metadata2)
    val commonIds = membersMetadata1.keys.intersect(membersMetadata2.keys).sorted()
    for (id in commonIds) {
        val member1 = membersMetadata1[id]!!
        val member2 = membersMetadata2[id]!!
        GenericMetadataTask(member1, member2, reportBuilder(id), checkers).run()
    }
}
