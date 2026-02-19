/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.KmPackage
import org.jetbrains.kotlin.abicmp.reports.PackageMetadataReport

abstract class PackageMetadataListChecker(name: String) : PackageMetadataChecker {
    override fun check(
        metadata1: KmPackage,
        metadata2: KmPackage,
        report: PackageMetadataReport,
    ) {
        val list1 = getList(metadata1)
        val list2 = getList(metadata2)

        val diff = compareLists(list1.sorted(), list2.sorted()) ?: return

        report.addMembersListDiffs(diff)
    }

    abstract fun getList(metadata: KmPackage): List<String>

    override val name: String = "class.metadata.$name"
}

fun fileFacadeMetadataListChecker(name: String, listGetter: (KmPackage) -> List<String>) =
    object : PackageMetadataListChecker(name) {
        override fun getList(metadata: KmPackage) = listGetter(metadata)
    }
