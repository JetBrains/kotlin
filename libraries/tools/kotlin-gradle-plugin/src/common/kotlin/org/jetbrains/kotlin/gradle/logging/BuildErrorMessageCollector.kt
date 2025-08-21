/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.File

/**
 * Collects build error messages and writes them to a file for error reporting.
 * This collector serves as a bridge between build tools and IDE error reporting systems.
 * [KotlinGradleBuildErrorsChecker.kt] in InteliJ is responsible for reading the file and   asking users to report the issue.
 *
 */

class BuildErrorMessageCollector(val logger: KotlinLogger, private val kotlinPluginVersion: String? = null) {
    private val errors = ArrayList<String>()

    fun clear() {
        synchronized(errors) {
            errors.clear()
        }
    }

    fun addError(error: String) {
        synchronized(errors) {
            errors.add(error)
        }
    }

    fun hasErrors() = synchronized(errors) {
        errors.isNotEmpty()
    }

    fun flush(files: Set<File>) {
        if (!hasErrors()) {
            return
        }
        errors.reportToIde(files, kotlinPluginVersion, logger = logger)
        clear()
    }
}