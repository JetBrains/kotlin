/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import java.io.File

val testDataDir = File("native/objcexport-header-generator/testData")
val headersTestDataDir = testDataDir.resolve("headers")
val baseDeclarationsDir = testDataDir.resolve("baseDeclarations")
val forwardDeclarationsDir = testDataDir.resolve("forwardDeclarations")
val dependenciesDir = testDataDir.resolve("dependencies")