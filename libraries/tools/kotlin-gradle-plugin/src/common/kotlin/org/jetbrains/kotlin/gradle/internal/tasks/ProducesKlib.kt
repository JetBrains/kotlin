/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.tasks

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import java.io.File

internal interface ProducesKlib : Task {
    @get:Internal
    val klibFile: Provider<File>
}