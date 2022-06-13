/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.serialize

interface IdeaKpmSerializationLogger {
    enum class Severity {
        WARNING,
        ERROR,
    }

    fun report(severity: Severity, message: String, cause: Throwable? = null)
    fun warn(message: String, cause: Throwable? = null) = report(Severity.WARNING, message, cause)
    fun error(message: String, cause: Throwable? = null) = report(Severity.ERROR, message, cause)

    object None : IdeaKpmSerializationLogger {
        override fun report(severity: Severity, message: String, cause: Throwable?) = Unit
    }
}
