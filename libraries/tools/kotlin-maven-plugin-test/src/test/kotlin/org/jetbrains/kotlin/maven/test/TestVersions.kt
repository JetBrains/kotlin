/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

interface TestVersions {
    enum class Java(
        val numericVersion: Int
    ) {
        JDK_1_8(8),
        JDK_11(11),
        JDK_17(17),
        JDK_21(21)
    }
}