/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.reports.MultiFileClassFacadeMetadataReport

abstract class MultiFileClassFacadeMetadataListChecker(name: String) : MultiFileClassFacadeMetadataChecker {
    override fun check(
        metadata1: KotlinClassMetadata.MultiFileClassFacade,
        metadata2: KotlinClassMetadata.MultiFileClassFacade,
        report: MultiFileClassFacadeMetadataReport,
    ) {
        val list1 = getList(metadata1)
        val list2 = getList(metadata2)

        val diff = compareLists(list1.sorted(), list2.sorted()) ?: return

        report.addMembersListDiffs(diff)
    }

    abstract fun getList(metadata: KotlinClassMetadata.MultiFileClassFacade): List<String>

    override val name: String = "class.metadata.$name"
}

fun multiFileClassFacadeMetadataListChecker(name: String, listGetter: (KotlinClassMetadata.MultiFileClassFacade) -> List<String>) =
    object : MultiFileClassFacadeMetadataListChecker(name) {
        override fun getList(metadata: KotlinClassMetadata.MultiFileClassFacade) = listGetter(metadata)
    }
