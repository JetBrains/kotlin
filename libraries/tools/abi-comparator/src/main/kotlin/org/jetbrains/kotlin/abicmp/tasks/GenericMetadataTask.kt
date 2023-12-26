/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import org.jetbrains.kotlin.abicmp.checkers.GenericMetadataChecker
import org.jetbrains.kotlin.abicmp.reports.MetadataPropertyReport

class GenericMetadataTask<T>(
    private val metadata1: T,
    private val metadata2: T,
    private val report: MetadataPropertyReport,
    private val checkers: List<GenericMetadataChecker<T>>
) : Runnable {
    override fun run() {
        for (checker in checkers) {
            checker.check(metadata1, metadata2, report)
        }
    }
}