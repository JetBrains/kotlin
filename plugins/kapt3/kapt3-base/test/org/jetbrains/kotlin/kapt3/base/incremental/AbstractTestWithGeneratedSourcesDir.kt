/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class AbstractTestWithGeneratedSourcesDir {
    @TempDir
    private var _generatedSources: File? = null

    protected val generatedSources: File
        get() = _generatedSources!!
}
