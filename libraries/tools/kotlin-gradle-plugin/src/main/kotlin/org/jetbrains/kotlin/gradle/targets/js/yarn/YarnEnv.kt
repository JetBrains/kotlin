/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File
import java.io.Serializable

data class YarnEnv(
    val downloadUrl: String,
    val cleanableStore: CleanableStore,
    val home: File,
    val executable: String,
    val ivyDependency: String,
    val standalone: Boolean,
    val ignoreScripts: Boolean,
) : Serializable
