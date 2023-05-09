/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project

internal val Project.androidExtensionOrNull: BaseExtension? get() = extensions.findByName("android")?.let { it as? BaseExtension }

internal val Project.androidExtension: BaseExtension get() = extensions.getByName("android") as BaseExtension

internal val Project.hasAndroidPlugin : Boolean get() = extensions.findByName("android") != null