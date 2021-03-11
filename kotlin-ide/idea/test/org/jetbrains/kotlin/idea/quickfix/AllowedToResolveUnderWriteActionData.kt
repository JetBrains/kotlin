/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import java.io.File
import java.util.*

class AllowedToResolveUnderWriteActionData(path: String, private val comment: String) {
    private val file = File(path)

    private val allowedActions = if (file.exists()) {
        file.readLines().filter {
            val trimmed = it.trim()
            !(trimmed.isEmpty() || trimmed.startsWith("#"))
        }.toSet()
    } else {
        setOf()
    }

    // Set to true to update test data.
    private val updateTestData = false
    private val updateTestDataSet: MutableSet<String> = TreeSet(allowedActions)

    fun isWriteActionAllowed(action: String): Boolean {
        if (action in allowedActions) {
            return true
        }

        @Suppress("ConstantConditionIf")
        if (updateTestData && updateTestDataSet.add(action)) {
            file.writeText(
                "$comment\n\n${updateTestDataSet.joinToString("\n")}"
            )
        }

        return false
    }
}