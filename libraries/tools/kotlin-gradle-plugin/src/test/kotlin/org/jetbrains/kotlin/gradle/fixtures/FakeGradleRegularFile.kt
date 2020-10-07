/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fixtures

import org.gradle.api.file.RegularFile
import java.io.File

class FakeGradleRegularFile(private val file: File) : RegularFile {
    override fun getAsFile(): File = file
}