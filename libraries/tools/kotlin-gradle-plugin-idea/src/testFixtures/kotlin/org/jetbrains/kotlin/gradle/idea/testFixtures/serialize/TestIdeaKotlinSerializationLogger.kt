/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationLogger
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationLogger.Severity
import java.io.Serializable

class TestIdeaKotlinSerializationLogger : IdeaKotlinSerializationLogger {
    data class Report(
        val severity: Severity,
        val message: String,
        val cause: Throwable? = null
    ) : Serializable {
        override fun toString(): String = "[${severity.name}]: $message"
    }

    private val _reports = mutableListOf<Report>()

    val reports get() = _reports.toList()

    override fun report(severity: Severity, message: String, cause: Throwable?) {
        _reports.add(Report(severity, message, cause))
    }
}
