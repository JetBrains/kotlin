/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arch.config

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import java.io.File
import kotlin.io.path.Path

val kgpClasses: JavaClasses = ClassFileImporter().importPaths(
    System.getProperty("archUnitClasses")
        .split(File.pathSeparator)
        .filter { it.isNotEmpty() }
        .map { Path(it) }
)
