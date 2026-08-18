/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.kotlin.sample

import org.jetbrains.dokka.analysis.test.api.TestDataFile

class KotlinSampleTestDataFile(
    pathFromProjectRoot: String,
    fqPackageName: String,
) : TestDataFile(pathFromProjectRoot = pathFromProjectRoot) {

    private var fileContents = "package $fqPackageName" + System.lineSeparator().repeat(2)

    operator fun String.unaryPlus() {
        fileContents += (this.trimIndent() + System.lineSeparator())
    }

    override fun getContents(): String {
        return fileContents
    }

    override fun toString(): String {
        return "KotlinSampleTestDataFile(pathFromProjectRoot='$pathFromProjectRoot', fileContents='$fileContents')"
    }
}
