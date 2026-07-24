/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import java.io.File

internal val konanHome: File by lazy {
    System.getProperty("konan.home")?.let { File(it) }
        ?: throw IllegalStateException("konan.home system property is not set")
}
