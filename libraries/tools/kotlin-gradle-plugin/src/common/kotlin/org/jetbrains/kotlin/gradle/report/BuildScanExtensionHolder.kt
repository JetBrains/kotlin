/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.gradle.scan.plugin.BuildScanExtension

class BuildScanExtensionHolder(val buildScan: BuildScanExtension) : java.io.Serializable {
    constructor(extension: Any) : this(extension as BuildScanExtension)
}