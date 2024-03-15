/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.klib.reader.testUtils

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

val providedTestKlibs: Set<Path> = System.getProperty("testKlibs").let { testKlibs ->
    if (testKlibs == null) error("Missing 'testKlibs' System property")
    testKlibs.split(File.pathSeparator).map(::Path).toSet()
}

val providedTestProjectKlib = providedTestKlibs.find { path ->
    path.contains(Path("testProject"))
} ?: error("Missing 'testProject' klib")