/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import java.io.File

internal data class UklibFragment(
    val identifier: String,
    val attributes: Set<String>,
    /**
     * FIXME: This file is a lambda to
     * - capture Provider<File> when the fragment is packed in KGP
     * - return the file on disk when Uklib fragment is deserialized from disk
     *
     * Maybe we should just have the Provider<UklibFragment> in KGP and this would then be a File property
     */
    val file: () -> File,
)
