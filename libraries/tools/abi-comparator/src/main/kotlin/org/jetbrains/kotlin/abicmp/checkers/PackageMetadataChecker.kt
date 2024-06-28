/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import kotlin.metadata.KmPackage
import org.jetbrains.kotlin.abicmp.reports.PackageMetadataReport

interface PackageMetadataChecker : Checker {
    fun check(metadata1: KmPackage, metadata2: KmPackage, report: PackageMetadataReport)
}
