/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import java.io.Serializable

enum class BuildReportType : Serializable {
    FILE,
    HTTP,
    BUILD_SCAN;

    companion object {
        const val serialVersionUID: Long = 0
    }
}